/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.TurnTimer;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.common.*;
import megamek.common.GameTurn.UnloadStrandedTurn;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.AirmechRamAttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.AbstractOptions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.AbstractPathFinder;
import megamek.common.pathfinder.LongestPathFinder;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovementDisplay extends StatusBarPhaseDisplay {
    private static final long serialVersionUID = -7246715124042905688L;
    
    // Defines for the different flags
    public static final int CMD_NONE = 0;
    public static final int CMD_MECH = 1;
    public static final int CMD_TANK = 1 << 1;
    public static final int CMD_VTOL = 1 << 2;
    public static final int CMD_INF = 1 << 3;
    public static final int CMD_AERO = 1 << 4;
    public static final int CMD_AERO_VECTORED = 1 << 5;
    public static final int CMD_CONVERTER = 1 << 6;
    public static final int CMD_AIRMECH = 1 << 7;
    // Command used only in menus and has no associated button
    public static final int CMD_NO_BUTTON = 1 << 8;
    // Convenience defines for common combinations
    public static final int CMD_AERO_BOTH = CMD_AERO | CMD_AERO_VECTORED;
    public static final int CMD_GROUND = CMD_MECH | CMD_TANK | CMD_VTOL | CMD_INF;
    public static final int CMD_NON_VECTORED = CMD_MECH | CMD_TANK | CMD_VTOL | CMD_INF | CMD_AERO;
    public static final int CMD_ALL = CMD_MECH | CMD_TANK | CMD_VTOL | CMD_INF | CMD_AERO | CMD_AERO_VECTORED;
    public static final int CMD_NON_INF = CMD_MECH | CMD_TANK | CMD_VTOL | CMD_AERO | CMD_AERO_VECTORED;
    private TurnTimer tt;

    /**
     * This enumeration lists all of the possible ActionCommands that can be carried out during the
     * movement phase. Each command has a string for the command plus a flag that determines what
     * unit type it is appropriate for.
     *
     * @author arlith
     */
    public enum MoveCommand implements PhaseCommand {
        MOVE_NEXT("moveNext", CMD_NONE),
        MOVE_TURN("moveTurn", CMD_GROUND | CMD_AERO),
        MOVE_WALK("moveWalk", CMD_GROUND),
        MOVE_JUMP("moveJump", CMD_MECH | CMD_TANK | CMD_INF),
        MOVE_BACK_UP("moveBackUp", CMD_MECH | CMD_TANK | CMD_VTOL),
        MOVE_GET_UP("moveGetUp", CMD_MECH),
        MOVE_FORWARD_INI("moveForwardIni", CMD_ALL),
        MOVE_CHARGE("moveCharge", CMD_MECH | CMD_TANK),
        MOVE_DFA("moveDFA", CMD_MECH),
        MOVE_GO_PRONE("moveGoProne", CMD_MECH),
        MOVE_FLEE("moveFlee", CMD_ALL),
        MOVE_EJECT("moveEject", CMD_ALL),
        MOVE_LOAD("moveLoad", CMD_MECH | CMD_TANK | CMD_VTOL),
        MOVE_UNLOAD("moveUnload", CMD_MECH | CMD_TANK | CMD_VTOL),
        MOVE_MOUNT("moveMount", CMD_GROUND),
        MOVE_TOW("moveTow", CMD_TANK),
        MOVE_DISCONNECT("moveDisconnect", CMD_TANK),
        MOVE_UNJAM("moveUnjam", CMD_NON_INF),
        MOVE_CLEAR("moveClear", CMD_INF),
        MOVE_CANCEL("moveCancel", CMD_NONE),
        MOVE_RAISE_ELEVATION("moveRaiseElevation", CMD_NON_VECTORED),
        MOVE_LOWER_ELEVATION("moveLowerElevation", CMD_NON_VECTORED),
        MOVE_SEARCHLIGHT("moveSearchlight", CMD_GROUND),
        MOVE_LAY_MINE("moveLayMine", CMD_TANK | CMD_INF),
        MOVE_HULL_DOWN("moveHullDown", CMD_MECH | CMD_TANK),
        MOVE_CLIMB_MODE("moveClimbMode", CMD_MECH | CMD_TANK | CMD_INF),
        MOVE_SWIM("moveSwim", CMD_MECH),
        MOVE_SHAKE_OFF("moveShakeOff", CMD_TANK | CMD_VTOL),
        MOVE_BRACE("moveBrace", CMD_MECH),
        // Convert command for a single button, which can cycle through modes because MovePath state is available
        MOVE_MODE_CONVERT("moveModeConvert", CMD_CONVERTER),
        // Convert commands used for menus, where the MovePath state is unknown.
        MOVE_MODE_LEG("moveModeLeg", CMD_NO_BUTTON),
        MOVE_MODE_VEE("moveModeVee", CMD_NO_BUTTON),
        MOVE_MODE_AIR("moveModeAir", CMD_NO_BUTTON),
        MOVE_RECKLESS("moveReckless", CMD_MECH | CMD_TANK | CMD_VTOL),
        MOVE_CAREFUL_STAND("moveCarefulStand", CMD_NONE),
        MOVE_EVADE("MoveEvade", CMD_MECH | CMD_TANK | CMD_VTOL),
        MOVE_BOOTLEGGER("moveBootlegger", CMD_TANK | CMD_VTOL),
        MOVE_SHUTDOWN("moveShutDown", CMD_NON_INF),
        MOVE_STARTUP("moveStartup", CMD_NON_INF),
        MOVE_SELF_DESTRUCT("moveSelfDestruct", CMD_NON_INF),
        // Infantry only
        MOVE_DIG_IN("moveDigIn", CMD_INF),
        MOVE_FORTIFY("moveFortify", CMD_INF),
        MOVE_TAKE_COVER("moveTakeCover", CMD_INF),
        MOVE_CALL_SUPPORT("moveCallSuport", CMD_INF),
        // VTOL attacks, declared in the movement phase
        MOVE_STRAFE("moveStrafe", CMD_VTOL),
        MOVE_BOMB("moveBomb", CMD_VTOL | CMD_AIRMECH),
        // Aero Movement
        MOVE_ACC("MoveAccelerate", CMD_AERO),
        MOVE_DEC("MoveDecelerate", CMD_AERO),
        MOVE_EVADE_AERO("MoveEvadeAero", CMD_AERO_BOTH),
        MOVE_ACCN("MoveAccNext", CMD_AERO),
        MOVE_DECN("MoveDecNext", CMD_AERO),
        MOVE_ROLL("MoveRoll", CMD_AERO_BOTH),
        MOVE_LAUNCH("MoveLaunch", CMD_AERO_BOTH),
        MOVE_DOCK("MoveDock", CMD_AERO_BOTH),
        MOVE_RECOVER("MoveRecover", CMD_AERO_BOTH),
        MOVE_DROP("MoveDrop", CMD_AERO_BOTH),
        MOVE_DUMP("MoveDump", CMD_AERO_BOTH),
        MOVE_RAM("MoveRam", CMD_AERO_BOTH | CMD_AIRMECH),
        MOVE_HOVER("MoveHover", CMD_AERO | CMD_AIRMECH),
        MOVE_MANEUVER("MoveManeuver", CMD_AERO_BOTH),
        MOVE_JOIN("MoveJoin", CMD_AERO_BOTH),
        MOVE_FLY_OFF("MoveOff", CMD_AERO_BOTH),
        MOVE_TAKE_OFF("MoveTakeOff", CMD_TANK),
        MOVE_VERT_TAKE_OFF("MoveVertTakeOff", CMD_TANK),
        MOVE_LAND("MoveLand", CMD_AERO_BOTH),
        MOVE_VERT_LAND("MoveVLand", CMD_AERO_BOTH),
        // Aero Vector Movement
        MOVE_TURN_LEFT("MoveTurnLeft", CMD_AERO_VECTORED),
        MOVE_TURN_RIGHT("MoveTurnRight", CMD_AERO_VECTORED),
        MOVE_THRUST("MoveThrust", CMD_AERO_VECTORED),
        MOVE_YAW("MoveYaw", CMD_AERO_VECTORED),
        MOVE_END_OVER("MoveEndOver", CMD_AERO_VECTORED),
        // Move envelope
        MOVE_ENVELOPE("MoveEnvelope", CMD_NONE),
        MOVE_LONGEST_RUN("MoveLongestRun", CMD_NONE),
        MOVE_LONGEST_WALK("MoveLongestWalk", CMD_NONE),
        // Traitor
        MOVE_TRAITOR("Traitor", CMD_NONE),
        MOVE_MORE("MoveMore", CMD_NONE);

        /**
         * The command text.
         */
        public final String cmd;

        /**
         * Flag that determines what unit types can use a command.
         */
        public final int flag;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        MoveCommand(String c, int f) {
            cmd = c;
            flag = f;
            priority = ordinal();
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
            return Messages.getString("MovementDisplay." + getCmd());
        }

        /**
         * Return a list of valid commands for the given parameters.
         *
         * @param f          The unit flag to specify what unit type the commands are
         *                   for.
         * @param opts       A GameOptions reference for checking game options
         * @param forwardIni A flag to see if we can pass the turn to a teammate
         * @return An array of valid commands for the given parameters
         */
        public static MoveCommand[] values(int f, GameOptions opts, boolean forwardIni) {
            boolean manualShutdown = false, selfDestruct = false, advVehicle = false, vtolStrafe = false;
            if (opts != null) {
                manualShutdown = opts.booleanOption(OptionsConstants.RPG_MANUAL_SHUTDOWN);
                selfDestruct = opts.booleanOption(OptionsConstants.ADVANCED_TACOPS_SELF_DESTRUCT);
                advVehicle = opts.booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS);
                vtolStrafe = opts.booleanOption(OptionsConstants.ADVCOMBAT_VTOL_STRAFING);
            }
            ArrayList<MoveCommand> flaggedCmds = new ArrayList<>();
            for (MoveCommand cmd : MoveCommand.values()) {
                // Check for movements that with disabled game options
                if (((cmd == MOVE_SHUTDOWN) || (cmd == MOVE_STARTUP)) && !manualShutdown) {
                    continue;
                } else if ((cmd == MOVE_SELF_DESTRUCT) && !selfDestruct) {
                    continue;
                } else if ((cmd == MOVE_FORWARD_INI) && !forwardIni) {
                    continue;
                } else if ((cmd == MOVE_BOOTLEGGER) && !advVehicle) {
                    continue;
                } else if ((cmd == MOVE_STRAFE) && !vtolStrafe) {
                    continue;
                }

                // Check unit type flag
                if ((cmd.flag & f) != 0) {
                    flaggedCmds.add(cmd);
                }
            }
            return flaggedCmds.toArray(new MoveCommand[0]);
        }
    }

    // buttons
    private Map<MoveCommand, MegamekButton> buttons;

    // let's keep track of what we're moving, too
    private int cen = Entity.NONE; // current entity number
    private MovePath cmd; // considering movement data

    // what "gear" is our mech in?
    private int gear;

    // is the shift key held?
    private boolean shiftheld;

    /**
     * A local copy of the current entity's loaded units.
     */
    private List<Entity> loadedUnits = null;
    
    /**
     * A local copy of the current entity's towed trailers.
     */
    private List<Entity> towedUnits = null;

    public static final int GEAR_LAND = 0;
    public static final int GEAR_BACKUP = 1;
    public static final int GEAR_JUMP = 2;
    public static final int GEAR_CHARGE = 3;
    public static final int GEAR_DFA = 4;
    public static final int GEAR_TURN = 5;
    public static final int GEAR_SWIM = 6;
    public static final int GEAR_RAM = 7;
    public static final int GEAR_IMMEL = 8;
    public static final int GEAR_SPLIT_S = 9;
    public static final int GEAR_LONGEST_RUN = 10;
    public static final int GEAR_LONGEST_WALK = 11;
    public static final int GEAR_STRAFE = 12;

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public MovementDisplay(final ClientGUI clientgui) {
        super(clientgui);

        this.clientgui = clientgui;
        if (clientgui != null) {
            clientgui.getClient().getGame().addGameListener(this);
            clientgui.getBoardView().addBoardViewListener(this);
            clientgui.getClient().getGame().setupTeams();
            clientgui.getBoardView().addKeyListener(this);
        }

        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase"));

        // Create all of the buttons
        buttons = new HashMap<>((int) (MoveCommand.values().length * 1.25 + 0.5));
        for (MoveCommand cmd : MoveCommand.values()) {
            String title = Messages.getString("MovementDisplay." + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title, UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "MovementDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(clientgui == null);
            buttons.put(cmd, newButton);
        }

        butDone.setText("<html><b>"
                        + Messages.getString("MovementDisplay.butDone") + "</b></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

        gear = MovementDisplay.GEAR_LAND;
        shiftheld = false;
        
        registerKeyCommands();
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    private void registerKeyCommands() {
        if (clientgui == null) {
            return;
        }

        MegaMekController controller = clientgui.controller;

        if (controller == null) {
            return;
        }

        final StatusBarPhaseDisplay display = this;
        // Register the action for TURN_LEFT
        controller.registerCommandAction(KeyCommandBind.TURN_LEFT.cmd,
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
                        int dir = cmd.getFinalFacing();
                        dir = (dir + 5) % 6;
                        Coords curPos = cmd.getFinalCoords();
                        Coords target = curPos.translated(dir);
                        // We need to set this to get the rotate behavior
                        shiftheld = true;
                        currentMove(target);
                        shiftheld = false;
                        clientgui.getBoardView().drawMovementData(ce(), cmd);
                    }
                });

        // Register the action for TURN_RIGHT
        controller.registerCommandAction(KeyCommandBind.TURN_RIGHT.cmd,
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
                        int dir = cmd.getFinalFacing();
                        dir = (dir + 7) % 6;
                        Coords curPos = cmd.getFinalCoords();
                        Coords target = curPos.translated(dir);
                        // We need to set this to get the rotate behavior
                        shiftheld = true;
                        currentMove(target);
                        shiftheld = false;
                        clientgui.getBoardView().drawMovementData(ce(), cmd);
                    }
                });

        // Register the action for UNDO
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP.cmd,
                new CommandAction() {
                    @Override
                    public boolean shouldPerformAction() {
                        return clientgui.getClient().isMyTurn()
                                && !clientgui.getBoardView().getChatterBoxActive()
                                && !display.isIgnoringEvents()
                                && display.isVisible();
                    }

                    @Override
                    public void performAction() {
                        removeLastStep();
                        computeMovementEnvelope(ce());
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
                        selectEntity(clientgui.getClient()
                                .getPrevEntityNum(cen));
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
                        computeMovementEnvelope(ce());
                    }
                });
        
        // Command to toggle between jumping and walk/run
        controller.registerCommandAction(KeyCommandBind.TOGGLE_MOVEMODE.cmd,
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
                        final Entity ce = ce();
                        boolean isAero = ce.isAero();
                        // first check if jumping is available at all
                        if (!isAero && !ce.isImmobile() && (ce.getJumpMP() > 0)
                                && !(ce.isStuck() && !ce.canUnstickByJumping())) {
                            if (gear != MovementDisplay.GEAR_JUMP) {
                                if (!((cmd.getLastStep() != null)
                                        && cmd.getLastStep().isFirstStep() 
                                        && (cmd.getLastStep().getType() == MoveStepType.LAY_MINE))) {
                                    clear();
                                }
                                if (!cmd.isJumping()) {
                                    cmd.addStep(MoveStepType.START_JUMP);
                                }
                                gear = MovementDisplay.GEAR_JUMP;
                                Color jumpColor = GUIPreferences.getInstance().getColor(
                                        GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
                                clientgui.getBoardView().setHighlightColor(jumpColor);
                            } else {
                                Color walkColor = GUIPreferences.getInstance().getColor(
                                        GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
                                clientgui.getBoardView().setHighlightColor(walkColor);
                                gear = MovementDisplay.GEAR_LAND;
                                clear();
                            }
                        } else {
                            Color walkColor = GUIPreferences.getInstance().getColor(
                                    GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
                            clientgui.getBoardView().setHighlightColor(walkColor);
                            gear = MovementDisplay.GEAR_LAND;
                            clear();
                        }
                        computeMovementEnvelope(ce); 
                    }
        });

        // Register the action for mode conversion
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CONVERSIONMODE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        return clientgui.getClient().isMyTurn()
                                && !clientgui.getBoardView().getChatterBoxActive()
                                && !display.isIgnoringEvents() && display.isVisible();
                    }

                    @Override
                    public void performAction() {
                        final Entity ce = ce();
                        if (ce == null) {
                            LogManager.getLogger().error("Cannot execute a conversion mode command for a null entity.");
                            return;
                        }
                        EntityMovementMode nextMode = ce.nextConversionMode(cmd.getFinalConversionMode());
                        // LAMs may have to skip the next mode due to damage
                        if (ce() instanceof LandAirMech) {
                            if (!((LandAirMech) ce).canConvertTo(nextMode)) {
                                nextMode = ce.nextConversionMode(nextMode);
                            }

                            if (!((LandAirMech) ce).canConvertTo(nextMode)) {
                                nextMode = ce.getMovementMode();
                            }
                        }
                        adjustConvertSteps(nextMode);
                        clientgui.getBoardView().drawMovementData(ce(), cmd);
                    }
                });
    }

    /**
     * @return the button list: we need to determine what unit type is selected and then get a
     * button list appropriate for that unit.
     */
    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        final Entity ce = ce();
        int flag = CMD_MECH;
        if (ce != null) {
            if (ce instanceof Infantry) {
                flag = CMD_INF;
            } else if (ce instanceof VTOL) {
                flag = CMD_VTOL;
            } else if (ce instanceof Tank) {
                flag = CMD_TANK;
            } else if (ce.isAero()) {
                if (ce.isAirborne()) {
                    flag = clientgui.getClient().getGame().useVectorMove()
                            ? CMD_AERO_VECTORED : CMD_AERO;
                } else {
                    flag = CMD_TANK;
                }

                if (ce instanceof LandAirMech) {
                    flag |= CMD_CONVERTER;
                }
            } else if (ce instanceof QuadVee) {
                if (ce.getConversionMode() == QuadVee.CONV_MODE_MECH) {
                    flag = CMD_MECH | CMD_CONVERTER;
                } else {
                    flag = CMD_TANK | CMD_CONVERTER;
                }
            } else if (ce instanceof LandAirMech) {
                if (ce.getConversionMode() == LandAirMech.CONV_MODE_AIRMECH) {
                    flag = CMD_TANK | CMD_CONVERTER | CMD_AIRMECH;
                } else {
                    flag = CMD_MECH | CMD_CONVERTER;
                }
            } else if ((ce instanceof Mech) && ((Mech) ce).hasTracks()) {
                flag = CMD_MECH | CMD_CONVERTER;
            } else if ((ce instanceof Protomech) && ce.getMovementMode().isWiGE()) {
                flag = CMD_MECH | CMD_AIRMECH;
            }
        }
        return getButtonList(flag);
    }

    private ArrayList<MegamekButton> getButtonList(int flag) {
        boolean forwardIni = false;
        GameOptions opts = null;
        if (clientgui != null) {
            Game game = clientgui.getClient().getGame();
            Player localPlayer = clientgui.getClient().getLocalPlayer();
            forwardIni = (game.getTeamForPlayer(localPlayer) != null)
                    && (game.getTeamForPlayer(localPlayer).getSize() > 1);
            opts = game.getOptions();
        }

        ArrayList<MegamekButton> buttonList = new ArrayList<>();

        int i = 0;
        MoveCommand[] commands = MoveCommand.values(flag, opts, forwardIni);
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (MoveCommand cmd : commands) {
            if (i % buttonsPerGroup == 0) {
                buttonList.add(getBtn(MoveCommand.MOVE_NEXT));
                i++;
            }

            buttonList.add(buttons.get(cmd));
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
     * Hands over the current turn to the next valid player on the same team as
     * the supplied player. If no player on the team apart from this player has
     * any turns left it activates this player again.
     */
    public synchronized void selectNextPlayer() {
        clientgui.getClient().sendNextPlayer();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public synchronized void selectEntity(int en) {
        final Entity ce = clientgui.getClient().getGame().getEntity(en);

        // hmm, sometimes this gets called when there's no ready entities?
        if (ce == null) {
            LogManager.getLogger().error("Tried to select non-existant entity with id " + en);
            return;
        }

        if (ce.isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce);
        }

        cen = en;
        clientgui.setSelectedEntityNum(en);
        gear = MovementDisplay.GEAR_LAND;
        Color walkColor = GUIPreferences.getInstance().getColor(GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
        clientgui.getBoardView().setHighlightColor(walkColor);
        clear();
        
        updateButtons();
        clientgui.getBoardView().highlight(ce.getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getUnitDisplay().displayEntity(ce);
        clientgui.getUnitDisplay().showPanel("movement");
        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.getBoardView().centerOnHex(ce.getPosition());
        }

        String yourTurnMsg = Messages.getString("MovementDisplay.its_your_turn");
        if (ce.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE)) {
            String poorPerfMsg;
            if (ce.getMpUsedLastRound() < ce.getWalkMP()) {
                poorPerfMsg = Messages.getString("MovementDisplay.NotUpToSpeed");
            } else {
                poorPerfMsg = Messages.getString("MovementDisplay.UpToSpeed");
            }
            setStatusBarText("<html><center>" + yourTurnMsg + "<br>" + poorPerfMsg + "</center></html>");
        } else {
            setStatusBarText(yourTurnMsg);
        }
        clientgui.getBoardView().clearFieldofF();
        computeMovementEnvelope(ce);
    }

    private MegamekButton getBtn(MoveCommand c) {
        return buttons.get(c);
    }

    private boolean isEnabled(MoveCommand c) {
        return buttons.get(c).isEnabled();
    }

    /**
     * Sets the buttons to their proper states
     */
    private void updateButtons() {
        final GameOptions gOpts = clientgui.getClient().getGame().getOptions();
        final Entity ce = ce();
        if (ce == null) {
            LogManager.getLogger().error("Cannot update buttons based on a null entity");
            return;
        }
        boolean isMech = (ce instanceof Mech);
        boolean isInfantry = (ce instanceof Infantry);
        boolean isAero = ce.isAero();

        if (numButtonGroups > 1) {
            getBtn(MoveCommand.MOVE_MORE).setEnabled(true);
        }

        setWalkEnabled(!ce.isImmobile() && ((ce.getWalkMP() > 0) || (ce.getRunMP() > 0))
                && !ce.isStuck());
        setJumpEnabled(!isAero && !ce.isImmobile() && !ce.isProne() && (ce.getJumpMP() > 0)
                && !(ce.isStuck() && !ce.canUnstickByJumping()));
        setSwimEnabled(!isAero && !ce.isImmobile() && (ce.getActiveUMUCount() > 0)
                && ce.isUnderwater());
        setBackUpEnabled(!isAero && isEnabled(MoveCommand.MOVE_WALK));
        setChargeEnabled(ce.canCharge());
        setDFAEnabled(ce.canDFA());
        setRamEnabled(ce.canRam());

        if (isInfantry) {
            setClearEnabled(clientgui.getClient().getGame().containsMinefield(ce.getPosition()));
        } else {
            setClearEnabled(false);
        }

        getBtn(MoveCommand.MOVE_CLIMB_MODE).setEnabled(Stream.of(EntityMovementMode.HYDROFOIL,
                        EntityMovementMode.NAVAL, EntityMovementMode.SUBMARINE,
                        EntityMovementMode.INF_UMU, EntityMovementMode.VTOL,
                        EntityMovementMode.BIPED_SWIM, EntityMovementMode.QUAD_SWIM)
                .noneMatch(entityMovementMode -> (ce.getMovementMode() == entityMovementMode)));
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

        // Infantry - Fortify
        if (isInfantry
            && ce.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_VIBROSHOVEL)) {
            // Crews adrift in space or atmosphere can't do this
            if (ce instanceof EjectedCrew && (ce.isSpaceborne() || ce.isAirborne())) {
                getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
            } else {
                getBtn(MoveCommand.MOVE_FORTIFY).setEnabled(true);
            }
        } else {
            getBtn(MoveCommand.MOVE_FORTIFY).setEnabled(false);
        }
        // Infantry - Digging in
        if (isInfantry && gOpts.booleanOption(OptionsConstants.ADVANCED_TACOPS_DIG_IN)) {
            // Crews adrift in space or atmosphere can't do this
            if (ce instanceof EjectedCrew && (ce.isSpaceborne() || ce.isAirborne())) {
                getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
            } else {
                // Allow infantry to dig in if they aren't currently dug in
                int dugInState = ((Infantry) ce).getDugIn();
                getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(
                        dugInState == Infantry.DUG_IN_NONE);
            }
        } else {
            getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
        }
        // Infantry - Take Cover
        // Crews adrift in space or atmosphere can't do this
        if (ce instanceof EjectedCrew && (ce.isSpaceborne() || ce.isAirborne())) {
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(false);
        } else {
            updateTakeCoverButton();
        }

        // Infantry - Urban Guerrilla calling for support
        if (isInfantry && ce.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA)
                && ((Infantry) ce).getCanCallSupport()) {
            getBtn(MoveCommand.MOVE_CALL_SUPPORT).setEnabled(true);
        } else {
            getBtn(MoveCommand.MOVE_CALL_SUPPORT).setEnabled(false);
        }

        getBtn(MoveCommand.MOVE_SHAKE_OFF).setEnabled(
                (ce instanceof Tank)
                && (ce.getSwarmAttackerId() != Entity.NONE));

        setFleeEnabled(ce.canFlee());
        if (gOpts.booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLES_CAN_EJECT) && (ce instanceof Tank)) {
            // Vehicle don't have ejection systems so crews abandon, and must enter a valid hex.
            // If they cannot, they can't abandon as per TO pg 197.
            Coords pos = ce().getPosition();
            Infantry inf = new Infantry();
            inf.setGame(clientgui.getClient().getGame());
            boolean hasLegalHex = !inf.isLocationProhibited(pos);
            for (int i = 0; i < 6; i++) {
                hasLegalHex |= !inf.isLocationProhibited(pos.translated(i));
            }
            setEjectEnabled(hasLegalHex);
        } else {
            setEjectEnabled(((isMech && (((Mech) ce).getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)) || isAero)
                    && ce.isActive()
                    && !ce.hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT));
        }

        // if dropping unit only allow turning
        if (!ce.isAero() && cmd.getFinalAltitude() > 0) {
            disableButtons();
            if (ce instanceof LandAirMech) {
                updateConvertModeButton();
                if (ce.getMovementMode() == EntityMovementMode.WIGE
                        && ce.getAltitude() <= 3) {
                    updateHoverButton();
                }
                getBtn(MoveCommand.MOVE_MORE).setEnabled(numButtonGroups > 1);
            }
            butDone.setEnabled(true);
        }


        // if small craft/dropship that has unloaded units, then only allowed
        // to unload more
        if (ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
        }

        setupButtonPanel();
    }
    
    private void updateAeroButtons() {
        if (ce() != null && ce().isAero()) {
            getBtn(MoveCommand.MOVE_THRUST).setEnabled(true);
            getBtn(MoveCommand.MOVE_YAW).setEnabled(true);
            getBtn(MoveCommand.MOVE_END_OVER).setEnabled(true);
            getBtn(MoveCommand.MOVE_TURN_LEFT).setEnabled(true);
            getBtn(MoveCommand.MOVE_TURN_RIGHT).setEnabled(true);
            setEvadeAeroEnabled(cmd != null && !cmd.contains(MoveStepType.EVADE));
            setEjectEnabled(true);
            // no turning for spheroids in atmosphere
            if ((((IAero) ce()).isSpheroid() || clientgui.getClient().getGame()
                    .getPlanetaryConditions().isVacuum())
                    && !clientgui.getClient().getGame().getBoard().inSpace()) {
                setTurnEnabled(false);
            }
        }
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getString("MovementDisplay.its_your_turn"));
        butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Done") + "</b></html>");
        butDone.setEnabled(true);
        setNextEnabled(true);
        setForwardIniEnabled(true);
        clientgui.getBoardView().clearFieldofF();
        if (numButtonGroups > 1) {
            getBtn(MoveCommand.MOVE_MORE).setEnabled(true);
        }

        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.maybeShowUnitDisplay();
        }
        selectEntity(clientgui.getClient().getFirstEntityNum());
        // check if there should be a turn timer running
        tt = TurnTimer.init(this, clientgui.getClient());
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private synchronized void endMyTurn() {
        final Entity ce = ce();

        //get rid of still running timer, if turn is concluded before time is up
        if (tt != null) {
            tt.stopTimer();
            tt = null;
        }

        // end my turn, then.
        disableButtons();
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if ((GamePhase.MOVEMENT == clientgui.getClient().getGame()
                .getPhase())
                && (null != next)
                && (null != ce)
                && (next.getOwnerId() != ce.getOwnerId())) {
            clientgui.setUnitDisplayVisible(false);
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        // Return the highlight sprite back to its original color
        clientgui.getBoardView().setHighlightColor(Color.white);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().selectEntity(null);
        clientgui.setSelectedEntityNum(Entity.NONE);
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFieldofF();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setWalkEnabled(false);
        setJumpEnabled(false);
        setBackUpEnabled(false);
        setTurnEnabled(false);
        setFleeEnabled(false);
        setFlyOffEnabled(false);
        setEjectEnabled(false);
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

        getBtn(MoveCommand.MOVE_CLIMB_MODE).setEnabled(false);
        getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
        getBtn(MoveCommand.MOVE_CALL_SUPPORT).setEnabled(false);
    }

    /**
     * Clears out the currently selected movement data and resets it.
     */
    @Override
    public void clear() {
        final Entity ce = ce();

        // clear board cursors
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearMovementEnvelope();

        if (ce == null) {
            return;
        }

        // Remove Careful stand, in case it was set
        ce.setCarefulStand(false);
        ce.setIsJumpingNow(false);
        ce.setConvertingNow(false);
        ce.setClimbMode(GUIPreferences.getInstance().getBoolean(GUIPreferences.ADVANCED_MOVE_DEFAULT_CLIMB_MODE));

        // switch back from swimming to normal mode.
        if (ce.getMovementMode() == EntityMovementMode.BIPED_SWIM) {
            ce.setMovementMode(EntityMovementMode.BIPED);
        } else if (ce.getMovementMode() == EntityMovementMode.QUAD_SWIM) {
            ce.setMovementMode(EntityMovementMode.QUAD);
        }
        
        // create new current and considered paths
        cmd = new MovePath(clientgui.getClient().getGame(), ce);
        clientgui.getBoardView().setWeaponFieldofFire(ce, cmd);

        // set to "walk," or the equivalent
        if (gear != MovementDisplay.GEAR_JUMP) {
            gear = MovementDisplay.GEAR_LAND;
            Color walkColor = GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
            clientgui.getBoardView().setHighlightColor(walkColor);
        } else if (!cmd.isJumping()) {
            cmd.addStep(MoveStepType.START_JUMP);
        }

        // update some GUI elements
        clientgui.getBoardView().clearMovementData();
        butDone.setText("<html><b>"
                        + Messages.getString("MovementDisplay.Done") + "</b></html>");
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

        loadedUnits = ce.getLoadedUnits();
        if (ce instanceof Aero) {
            for (Entity e : ce.getUnitsUnloadableFromBays()) {
                if (!loadedUnits.contains(e)) {
                    loadedUnits.add(e);
                }
            }
        }
        towedUnits = ce.getLoadedTrailers();

        updateLoadButtons();
        updateJoinButton();
        updateRecoveryButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();

        // if dropping unit only allow turning
        if (!ce.isAero() && cmd.getFinalAltitude() > 0) {
            disableButtons();
            if (ce instanceof LandAirMech) {
                updateConvertModeButton();
                if (ce.getMovementMode().isWiGE() && (ce.getAltitude() <= 3)) {
                    updateHoverButton();
                }
                getBtn(MoveCommand.MOVE_MORE).setEnabled(numButtonGroups > 1);
            } else {
                gear = MovementDisplay.GEAR_TURN;
            }
            butDone.setEnabled(true);
        }

        // If a Small Craft / DropShip that has unloaded units, then only allowed to unload more
        if (ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }
        
    }

    private void removeLastStep() {
        cmd.removeLastStep();
        final Entity entity = ce();
        if (entity == null) {
            LogManager.getLogger().warn("Cannot process removeLastStep for a null entity.");
            return;
        } else if (cmd.length() == 0) {
            clear();
            if ((gear == MovementDisplay.GEAR_JUMP) && !cmd.isJumping()) {
                cmd.addStep(MoveStepType.START_JUMP);
            } else if (entity.isConvertingNow()) {
                cmd.addStep(MoveStepType.CONVERT_MODE);
            }
        } else {
            // clear board cursors
            clientgui.getBoardView().select(cmd.getFinalCoords());
            clientgui.getBoardView().cursor(cmd.getFinalCoords());
            clientgui.getBoardView().drawMovementData(entity, cmd);
            clientgui.getBoardView().setWeaponFieldofFire(entity, cmd);

            // Set the button's label to "Done"
            // if the entire move is impossible.
            MovePath possible = cmd.clone();
            possible.clipToPossible();
            if (possible.length() == 0) {
                butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Done") + "</b></html>");
            }
        }
        updateButtons();
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    @Override
    public synchronized void ready() {
        if (ce() == null) {
            return;
        }

        cmd.clipToPossible();
        if ((cmd.length() == 0) && !ce().isAirborne()
            && GUIPreferences.getInstance().getNagForNoAction()) {
            // Hmm....no movement steps, comfirm this action
            String title = Messages
                    .getString("MovementDisplay.ConfirmNoMoveDlg.title");
            String body = Messages
                    .getString("MovementDisplay.ConfirmNoMoveDlg.message");
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        if (cmd.hasActiveMASC() && GUIPreferences.getInstance().getNagForMASC()) {
            // pop up are you sure dialog
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmMASCRoll", ce().getMASCTarget()),
                    true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForMASC(false);
                }
            }
        }

        if (cmd.hasActiveSupercharger() && GUIPreferences.getInstance().getNagForMASC()) {
            if (!(ce() instanceof VTOL)) {
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                        Messages.getString("MovementDisplay.areYouSure"),
                        Messages.getString("MovementDisplay.ConfirmSuperchargerRoll", ce().getSuperchargerTarget()),
                        true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        GUIPreferences.getInstance().setNagForMASC(false);
                    }
                } else {
                    return;
                }
            }
        }

        if ((cmd.getLastStepMovementType() == EntityMovementType.MOVE_SPRINT
                || cmd.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_SPRINT)
                && GUIPreferences.getInstance().getNagForSprint()
                // no need to nag for vehicles using overdrive if they already get a PSR nag
                && !((cmd.getEntity() instanceof Tank
                        || (cmd.getEntity() instanceof QuadVee
                                && cmd.getEntity().getConversionMode() == QuadVee.CONV_MODE_VEHICLE)
                        && GUIPreferences.getInstance().getNagForPSR()))) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmSprint"), true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForSprint(false);
                }
            } else {
                return;
            }
        }
        String check = SharedUtility.doPSRCheck(cmd);
        if (!check.isBlank() && GUIPreferences.getInstance().getNagForPSR()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmPilotingRoll") +
                            check, true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForPSR(false);
                }
            } else {
                return;
            }
        }

        // Should we nag about taking fall damage with mechanical jump boosters?
        if (cmd.shouldMechanicalJumpCauseFallDamage()
                && GUIPreferences.getInstance().getNagForMechanicalJumpFallDamage()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmMechanicalJumpFallDamage",
                            cmd.getJumpMaxElevationChange(), ce().getJumpMP(),
                            cmd.getJumpMaxElevationChange() - ce().getJumpMP()), true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForMechanicalJumpFallDamage(false);
                }
            } else {
                return;
            }
        }

        // check for G-forces
        check = SharedUtility.doThrustCheck(cmd, clientgui.getClient());
        if (!check.isBlank() && GUIPreferences.getInstance().getNagForPSR()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmPilotingRoll") + check,
                    true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForPSR(false);
                }
            } else {
                return;
            }
        }

        // check for unsafe takeoffs
        if (cmd.contains(MoveStepType.VTAKEOFF)
                || cmd.contains(MoveStepType.TAKEOFF)) {
            boolean unsecure = false;
            for (Entity loaded : ce().getLoadedUnits()) {
                if (loaded.wasLoadedThisTurn() && !(loaded instanceof Infantry)) {
                    unsecure = true;
                    break;
                }
            }
            if (unsecure) {
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                        Messages.getString("MovementDisplay.areYouSure"),
                        Messages.getString("MovementDisplay.UnsecuredTakeoff"), true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        GUIPreferences.getInstance().setNagForPSR(false);
                    }
                } else {
                    return;
                }
            }
        }

        // check to see if spheroids will drop elevation
        // they will do so if they're not hovering, landing, or changing altitude voluntarily.
        if (Compute.useSpheroidAtmosphere(clientgui.getClient().getGame(), ce()) 
                && !cmd.contains(MoveStepType.HOVER) 
                && !cmd.contains(MoveStepType.VLAND)
                && !cmd.contains(MoveStepType.UP)
                && !cmd.contains(MoveStepType.DOWN)) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.SpheroidAltitudeLoss") +
                   
                            check, true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForPSR(false);
                }
            } else {
                return;
            }
        }
        
        if (ce().isAirborne() || ce().isSpaceborne()) {
            if (!clientgui.getClient().getGame().useVectorMove()) {
                if (ce().isAero() && !((IAero) ce()).isOutControlTotal()) {
                    // check for underuse of velocity
                    boolean unusedVelocity = false;
                    if (null != cmd.getLastStep()) {
                        unusedVelocity = cmd.getLastStep().getVelocityLeft() > 0;
                    } else {
                        unusedVelocity = (((IAero) ce()).getCurrentVelocity() > 0) &&
                                (ce().delta_distance == 0);
                    }
                    boolean flyoff = false;
                    if ((null != cmd)
                            && (cmd.contains(MoveStepType.OFF) || cmd
                                    .contains(MoveStepType.RETURN))) {
                        flyoff = true;
                    }
                    boolean landing = false;
                    if ((null != cmd) && cmd.contains(MoveStepType.LAND)) {
                        landing = true;
                    }
                    boolean ejecting = false;
                    if ((null != cmd) && cmd.contains(MoveStepType.EJECT)) {
                        ejecting = true;
                    }
                    if (unusedVelocity && !flyoff && !landing && !ejecting) {
                        String title = Messages
                                .getString("MovementDisplay.VelocityLeft.title");
                        String body = Messages
                                .getString("MovementDisplay.VelocityLeft.message");
                        clientgui.doAlertDialog(title, body);
                        return;
                    }
                }
            }
            // depending on the rules and location (i.e. space v. atmosphere),
            // Aeros might need to have additional move steps tacked on
            // This must be done after all prompts, otherwise a user who cancels
            // will still have steps added to the movepath.
            cmd = SharedUtility.moveAero(cmd, clientgui.getClient());
        }

        if (cmd.willCrushBuildings()
            && GUIPreferences.getInstance().getNagForCrushingBuildings()) {
            ConfirmDialog nag = new ConfirmDialog(
                    clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmCrushingBuildings"),
                    true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForCrushingBuildings(false);
                }
            } else {
                return;
            }
        }

        // check to see if spheroids will drop an elevation
        if (ce() instanceof LandAirMech && ce().isAssaultDropInProgress()
                && cmd.getFinalConversionMode() == EntityMovementMode.AERODYNE) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.insufficientAltitudeForConversion") + check,
                    false);
            nag.setVisible(true);
            if (!nag.getAnswer()) {
                return;
            }
        }
        
        if ((ce() instanceof Infantry) && ((Infantry) ce()).hasMicrolite()
                && (ce().isAirborneVTOLorWIGE() || (ce().getElevation() != cmd.getFinalElevation()))
                && !cmd.contains(MoveStepType.FORWARDS) && !cmd.contains(MoveStepType.FLEE)
                && cmd.getFinalElevation() > 0
                && ce().getGame().getBoard().getHex(cmd.getFinalCoords())
                    .terrainLevel(Terrains.BLDG_ELEV) < cmd.getFinalElevation()
                && ce().getGame().getBoard().getHex(cmd.getFinalCoords())
                    .terrainLevel(Terrains.BRIDGE_ELEV) < cmd.getFinalElevation()) {
            String title = Messages.getString("MovementDisplay.MicroliteMove.title");
            String body = Messages.getString("MovementDisplay.MicroliteMove.message");
            clientgui.doAlertDialog(title, body);
            return;
        }
        
        if (cmd.automaticWiGELanding(true)
                && GUIPreferences.getInstance().getNagForWiGELanding()) {
            ConfirmDialog nag = new ConfirmDialog(
                    clientgui.frame,
                    Messages.getString("MovementDisplay.areYouSure"),
                    Messages.getString("MovementDisplay.ConfirmWiGELanding"),
                    true);
            nag.setVisible(true);
            if (nag.getAnswer()) {
                // do they want to be bothered again?
                if (!nag.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForWiGELanding(false);
                }
            } else {
                return;
            }
        }

        disableButtons();
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearMovementEnvelope();
        if (ce().hasUMU()) {
            clientgui.getClient().sendUpdateEntity(ce());
        }
        clientgui.getClient().moveEntity(cen, cmd);
        if (ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    /**
     * Returns the current entity.
     */
    private synchronized Entity ce() {
        if (clientgui != null) {
            return clientgui.getClient().getGame().getEntity(cen);
        } else {
            return null;
        }
    }
    
    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if (shiftheld || (gear == GEAR_TURN)) {
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false);
        } else if ((gear == GEAR_JUMP)
                   && (ce().getJumpType() == Mech.JUMP_BOOSTER)) {
            // Jumps with mechanical jump boosters are special
            Coords src;
            if (cmd.getLastStep() != null) {
                src = cmd.getLastStep().getPosition();
            } else {
                src = ce().getPosition();
            }
            int dir = src.direction(dest);
            int facing = ce().getFacing();
            // Adjust dir based upon facing
            // Java does mod different from how we want...
            dir = (((dir - facing) % 6) + 6) % 6;
            switch (dir) {
                case 0:
                    cmd.findSimplePathTo(dest, MoveStepType.FORWARDS,
                            src.direction(dest), ce().getFacing());
                    break;
                case 1:
                    cmd.findSimplePathTo(dest, MoveStepType.LATERAL_RIGHT,
                            src.direction(dest), ce().getFacing());
                    break;
                case 2:
                    // TODO: backwards lateral shifts are switched:
                    // LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
                    cmd.findSimplePathTo(dest,
                            MoveStepType.LATERAL_LEFT_BACKWARDS,
                            src.direction(dest), ce().getFacing());
                    break;
                case 3:
                    cmd.findSimplePathTo(dest, MoveStepType.BACKWARDS,
                            src.direction(dest), ce().getFacing());
                    break;
                case 4:
                    // TODO: backwards lateral shifts are switched:
                    // LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
                    cmd.findSimplePathTo(dest,
                            MoveStepType.LATERAL_RIGHT_BACKWARDS,
                            src.direction(dest), ce().getFacing());
                    break;
                case 5:
                    cmd.findSimplePathTo(dest, MoveStepType.LATERAL_LEFT,
                            src.direction(dest), ce().getFacing());
                    break;
            }
        } else if (gear == GEAR_STRAFE) {
            // Only set the steps that enter new hexes.
            int start = cmd.length();
            cmd.findPathTo(dest, MoveStepType.FORWARDS);
            // Skip turns at the beginning of the new part of the path unless we're extending
            // an existing strafing pattern.
            if (start > 0 && !cmd.getStep(start - 1).isStrafingStep()) {
                while (start < cmd.length()
                        && cmd.getStep(start).getType() != MoveStepType.FORWARDS) {
                    start++;
                }
            }
            for (int i = cmd.length() - 1; i >= start; i--) {
                cmd.setStrafingStep(cmd.getStep(i).getPosition());
            }
            cmd.compile(clientgui.getClient().getGame(), ce(), false);
            gear = GEAR_LAND;
        } else if ((gear == GEAR_LAND) || (gear == GEAR_JUMP)) {
            cmd.findPathTo(dest, MoveStepType.FORWARDS);
        } else if (gear == GEAR_BACKUP) {
            cmd.findPathTo(dest, MoveStepType.BACKWARDS);
        } else if (gear == GEAR_CHARGE) {
            cmd.findPathTo(dest, MoveStepType.CHARGE);
            // The path planner shouldn't actually add the charge step
            if (cmd.getFinalCoords().equals(dest)
                && (cmd.getLastStep().getType() != MoveStepType.CHARGE)) {
                cmd.removeLastStep();
                cmd.addStep(MoveStepType.CHARGE);
            }
        } else if (gear == GEAR_DFA) {
            cmd.findPathTo(dest, MoveStepType.DFA);
            // The path planner shouldn't actually add the DFA step
            if (cmd.getFinalCoords().equals(dest)
                && (cmd.getLastStep().getType() != MoveStepType.DFA)) {
                cmd.removeLastStep();
                cmd.addStep(MoveStepType.DFA);
            }
        } else if (gear == GEAR_SWIM) {
            cmd.findPathTo(dest, MoveStepType.SWIM);
        } else if (gear == GEAR_RAM) {
            cmd.findPathTo(dest, MoveStepType.FORWARDS);
        } else if (gear == GEAR_IMMEL) {
            cmd.addStep(MoveStepType.UP, true, true);
            cmd.addStep(MoveStepType.UP, true, true);
            cmd.addStep(MoveStepType.DEC, true, true);
            cmd.addStep(MoveStepType.DEC, true, true);
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), true);
            gear = GEAR_LAND;
        } else if (gear == GEAR_SPLIT_S) {
            cmd.addStep(MoveStepType.DOWN, true, true);
            cmd.addStep(MoveStepType.DOWN, true, true);
            cmd.addStep(MoveStepType.ACC, true, true);
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), true);
            gear = GEAR_LAND;
        }
        if (gear == GEAR_LONGEST_WALK || gear == GEAR_LONGEST_RUN) {
            int maxMp;
            MoveStepType stepType;
            if (gear == GEAR_LONGEST_WALK) {
                maxMp = ce().getWalkMP();
                stepType = MoveStepType.BACKWARDS;
                gear = GEAR_BACKUP;
            } else {
                maxMp = ce().getRunMPwithoutMASC();
                stepType = MoveStepType.FORWARDS;
                gear = GEAR_LAND;
            }

            LongestPathFinder lpf;
            if (ce().isAero()) {
                lpf = LongestPathFinder.newInstanceOfAeroPath(maxMp, ce().getGame());
            } else {
                lpf = LongestPathFinder.newInstanceOfLongestPath(maxMp, stepType, ce().getGame());
            }
            final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
            lpf.addStopCondition(new AbstractPathFinder.StopConditionTimeout<>(timeLimit * 4));

            lpf.run(cmd);
            MovePath lPath = lpf.getComputedPath(dest);
            if (lPath != null) {
                cmd = lPath;
            }
        }
        clientgui.getBoardView().setWeaponFieldofFire(ce(), cmd);
    }

    //
    // BoardListener
    //
    @Override
    public synchronized void hexMoused(BoardViewEvent b) {
        if (clientgui == null) {
            return;
        }

        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // don't make a movement path for aeros if advanced movement is on
        boolean nopath = (ce != null && ce.isAero())
                && clientgui.getClient().getGame().useVectorMove();

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn() || ((b.getButton() != MouseEvent.BUTTON1))) {
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
        Coords currPosition = cmd != null ? cmd.getFinalCoords()
                : ce != null ? ce.getPosition() : null;

        if ((b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && !nopath) {
            if (!b.getCoords().equals(currPosition) || shiftheld
                    || (gear == MovementDisplay.GEAR_TURN)) {
                clientgui.getBoardView().cursor(b.getCoords());
                // either turn or move
                if (ce != null) {
                    currentMove(b.getCoords());
                    clientgui.getBoardView().drawMovementData(ce, cmd);
                }
            }
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            Coords moveto = b.getCoords();
            clientgui.getBoardView().drawMovementData(ce, cmd);
            if (shiftheld || (gear == MovementDisplay.GEAR_TURN)) {
                butDone.setText("<html><b>"
                        + Messages.getString("MovementDisplay.Move")
                        + "</b></html>");

                // Set the button's label to "Done"
                // if the entire move is impossible.
                MovePath possible = cmd.clone();
                possible.clipToPossible();
                if (possible.length() == 0) {
                    butDone.setText("<html><b>"
                            + Messages.getString("MovementDisplay.Done")
                            + "</b></html>");
                   
                }
            } else {
                clientgui.getBoardView().select(b.getCoords());
            }

            if (gear == MovementDisplay.GEAR_RAM) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)
                        || !target.isAero()) {
                    clientgui.doAlertDialog(
                            Messages.getString("MovementDisplay.CantRam"),
                            Messages.getString("MovementDisplay.NoTarget"));
                    clear();
                    return;
                }

                // check if it's a valid ram
                // First I need to add moves to the path if advanced
                if (ce.isAero() && clientgui.getClient().getGame().useVectorMove()) {
                    cmd.clipToPossible();
                }

                cmd.addStep(MoveStepType.RAM);

                ToHitData toHit = new RamAttackAction(cen,
                        target.getTargetType(), target.getTargetId(),
                        target.getPosition()).toHit(clientgui.getClient().getGame(), cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // Determine how much damage the charger will take.
                    IAero ta = (IAero) target;
                    IAero ae = (IAero) ce;
                    int toAttacker = RamAttackAction.getDamageTakenBy(ae, (Entity) ta,
                            cmd.getSecondFinalPosition(ce.getPosition()),
                            cmd.getHexesMoved(), ta.getCurrentVelocity());
                    int toDefender = RamAttackAction.getDamageFor(ae, (Entity) ta,
                            cmd.getSecondFinalPosition(ce.getPosition()),
                            cmd.getHexesMoved(), ta.getCurrentVelocity());

                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(
                            Messages.getString("MovementDisplay.RamDialog.title", target.getDisplayName()),
                            Messages.getString("MovementDisplay.RamDialog.message", toHit.getValueAsString(),
                                    Compute.oddsAbove(toHit.getValue(), ce.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                                    toHit.getDesc(), toDefender, toHit.getTableDesc(), toAttacker))) {
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
                clientgui.doAlertDialog(
                        Messages.getString("MovementDisplay.CantRam"),
                        toHit.getDesc());
                clear();
                return;
            } else if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui.doAlertDialog(
                            Messages.getString(ce.isAirborneVTOLorWIGE()
                                    ? "MovementDisplay.CantRam" : "MovementDisplay.CantCharge"),
                            Messages.getString("MovementDisplay.NoTarget"));
                    clear();
                    computeMovementEnvelope(ce);
                    return;
                }

                // check if it's a valid charge
                ToHitData toHit = null;
                if (ce.isAirborneVTOLorWIGE()) {
                    toHit = new AirmechRamAttackAction(cen,
                            target.getTargetType(), target.getTargetId(),
                            target.getPosition()).toHit(clientgui.getClient().getGame(), cmd);
                } else {
                    toHit = new ChargeAttackAction(cen,
                            target.getTargetType(), target.getTargetId(),
                            target.getPosition()).toHit(clientgui.getClient().getGame(), cmd);
                }

                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // Determine how much damage the charger will take.
                    int toDefender = 0;
                    int toAttacker = 0;
                    if (ce.isAirborneVTOLorWIGE()) {
                        toAttacker = AirmechRamAttackAction.getDamageTakenBy(ce, target, cmd.getHexesMoved());
                        toDefender = AirmechRamAttackAction.getDamageFor(ce, cmd.getHexesMoved());
                    } else {
                        toDefender = ChargeAttackAction.getDamageFor(
                                        ce, clientgui.getClient().getGame().getOptions()
                                                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE),
                                        cmd.getHexesMoved());
                        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
                            Entity te = (Entity) target;
                            toAttacker = ChargeAttackAction.getDamageTakenBy(ce, te,
                                    clientgui.getClient().getGame().getOptions()
                                            .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE),
                                    cmd.getHexesMoved());
                        } else if ((target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                                   || (target.getTargetType() == Targetable.TYPE_BUILDING)) {
                            Building bldg = clientgui.getClient().getGame().getBoard().getBuildingAt(moveto);
                            toAttacker = ChargeAttackAction.getDamageTakenBy(ce, bldg, moveto);
                        }
                    }

                    String title = "MovementDisplay.ChargeDialog.title";
                    String msg = "MovementDisplay.ChargeDialog.message";
                    if (ce.isAirborneVTOLorWIGE()) {
                        title = "MovementDisplay.AirmechRamDialog.title";
                        msg = "MovementDisplay.AirmechRamDialog.message";
                    }
                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(Messages.getString(title, target.getDisplayName()),
                            Messages.getString(msg, toHit.getValueAsString(),
                                    Compute.oddsAbove(toHit.getValue()), toHit.getDesc(), toDefender,
                                    toHit.getTableDesc(), toAttacker))) {
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
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"),
                        toHit.getDesc());
                clear();
                computeMovementEnvelope(ce);
                return;
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui.doAlertDialog(
                            Messages.getString("MovementDisplay.CantDFA"),
                            Messages.getString("MovementDisplay.NoTarget"));
                    clear();
                    computeMovementEnvelope(ce);
                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = DfaAttackAction.toHit(clientgui.getClient().getGame(), cen, target, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if (clientgui.doYesNoDialog(
                            Messages.getString("MovementDisplay.DFADialog.title",
                                    target.getDisplayName()),
                            Messages.getString("MovementDisplay.DFADialog.message",
                                    toHit.getValueAsString(), Compute.oddsAbove(toHit.getValue()),
                                    toHit.getDesc(),
                                    DfaAttackAction.getDamageFor(ce, target.isConventionalInfantry()),
                                    toHit.getTableDesc(), DfaAttackAction.getDamageTakenBy(ce)))) {
                        // if they answer yes, DFA the target
                        cmd.getLastStep().setTarget(target);
                        ready();
                    } else {
                        // else clear movement
                        clear();
                    }
                    return;
                }
                // if not valid, tell why
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"), toHit.getDesc());
                clear();
                return;
            }
            butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Move") + "</b></html>");
            butDone.setEnabled(clientgui.getClient().isMyTurn());
            updateProneButtons();
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
        final Game game = clientgui.getClient().getGame();
        final GameOptions gOpts = game.getOptions();
        boolean isInfantry = (ce() instanceof Infantry);
        
        // Infantry - Taking Cover
        if (isInfantry && gOpts.booleanOption(OptionsConstants.ADVANCED_TACOPS_TAKE_COVER)) {
            // Determine the current position of the infantry
            Coords pos;
            int elevation;
            if (cmd == null) {
                pos = ce().getPosition();
                elevation = ce().getElevation();
            } else {
                pos = cmd.getFinalCoords();
                elevation = cmd.getFinalElevation();
            }
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(
                    Infantry.hasValidCover(game, pos, elevation));
        } else {
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(false);
        }
    }

    private synchronized void updateProneButtons() {
        final Entity ce = ce();
        if (ce == null) {
            setGetUpEnabled(false);
            setGoProneEnabled(false);
            setHullDownEnabled(false);
            return;
        }

        boolean isMech = ce instanceof Mech;

        if (cmd.getFinalProne()) {
            setGetUpEnabled(!ce.isImmobile() && !ce.isStuck());
            setGoProneEnabled(false);
            setHullDownEnabled(true);
        } else if (cmd.getFinalHullDown()) {
            if (isMech) {
                setGetUpEnabled(!ce.isImmobile() && !ce.isStuck()
                                && !((Mech) ce).cannotStandUpFromHullDown());
            } else {
                setGetUpEnabled(!ce.isImmobile() && !ce.isStuck());
            }
            setGoProneEnabled(!ce.isImmobile() && isMech && !ce.isStuck());
            setHullDownEnabled(false);
        } else {
            setGetUpEnabled(false);
            setGoProneEnabled(!ce.isImmobile() && isMech && !ce.isStuck()
                              && !(getBtn(MoveCommand.MOVE_GET_UP).isEnabled()));
            if (!(ce instanceof Tank) && !(ce instanceof QuadVee
                    && ce.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
                setHullDownEnabled(ce.canGoHullDown());
            } else {
                // So that vehicle can move and go hull-down, we have to
                // check if it's moved into a fortified position
                if (cmd.getLastStep() != null) {
                    boolean hullDownEnabled = clientgui.getClient()
                                                       .getGame().getOptions()
                                                       .booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
                    Hex occupiedHex = clientgui.getClient().getGame()
                                                .getBoard()
                                                .getHex(cmd.getLastStep().getPosition());
                    boolean fortifiedHex = occupiedHex
                            .containsTerrain(Terrains.FORTIFIED);
                    setHullDownEnabled(hullDownEnabled && fortifiedHex);
                } else {
                    // If there's queued up movement, we can call the
                    // canGoHullDown() method in the Tank class.
                    setHullDownEnabled(ce.canGoHullDown());
                }

            }
        }
    }

    private void updateRACButton() {
        final Entity ce = ce();
        if (ce == null) {
            return;
        }
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        setUnjamEnabled(ce.canUnjamRAC()
                && ((gear == MovementDisplay.GEAR_LAND)
                        || (gear == MovementDisplay.GEAR_TURN) 
                        || (gear == MovementDisplay.GEAR_BACKUP))
                && ((cmd.getMpUsed() <= ce.getWalkMP()) 
                        || (cmd.getLastStep().isOnlyPavement() 
                                && (cmd.getMpUsed() <= (ce.getWalkMP() + 1))))
                && !(opts.booleanOption(OptionsConstants.ADVANCED_TACOPS_TANK_CREWS)
                        && (cmd.getMpUsed() > 0) && (ce instanceof Tank) 
                        && (ce.getCrew().getSize() < 2)));
    }

    private void updateSearchlightButton() {
        final Entity ce = ce();
        if (ce == null) {
            return;
        }
        boolean isNight = clientgui.getClient().getGame().getPlanetaryConditions().isSearchlightEffective();
        setSearchlightEnabled(isNight && ce.hasSearchlight() && !cmd.contains(MoveStepType.SEARCHLIGHT),
                ce.isUsingSearchlight());
    }

    private synchronized void updateElevationButtons() {
        final Entity ce = ce();
        if (ce == null) {
            return;
        }

        if (ce.isAirborne()) {
            // then use altitude not elevation
            setRaiseEnabled(ce.canGoUp(cmd.getFinalAltitude(), cmd.getFinalCoords()));
            setLowerEnabled(ce.canGoDown(cmd.getFinalAltitude(), cmd.getFinalCoords()));
            return;
        }
        // WiGEs (and LAMs and glider protomechs) cannot go up if they've used ground movement.
        if (ce.getMovementMode().isWiGE() && !ce.isAirborneVTOLorWIGE() && (cmd.getMpUsed() > 0)
                && !cmd.contains(MoveStepType.UP)) {
            setRaiseEnabled(false);
        } else {
            setRaiseEnabled(ce.canGoUp(cmd.getFinalElevation(), cmd.getFinalCoords()));
        }
        setLowerEnabled(ce.canGoDown(cmd.getFinalElevation(), cmd.getFinalCoords()));
    }

    private synchronized void updateTakeOffButtons() {
        if ((null != cmd) && (cmd.length() > 0)) {
            // you can't take off if you have already moved
            // http://www.classicbattletech.com/forums/index.php?topic=54112.0
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
            return;
        }

        final Entity ce = ce();
        if (ce == null) {
            return;
        }

        if (ce.isAero()) {
            if (ce.isAirborne()) {
                setTakeOffEnabled(false);
                setVTakeOffEnabled(false);
            } else if (!ce.isShutDown()) {
                setTakeOffEnabled(((IAero) ce).canTakeOffHorizontally());
                setVTakeOffEnabled(((IAero) ce).canTakeOffVertically());
            }
        } else {
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
        }
    }

    private synchronized void updateLandButtons() {

        if ((null != cmd) && (cmd.length() > 0)) {
            // According to the link below, you can move in the air and then
            // land
            // but I have personal message to Welshman right now asking that
            // this be changed
            // because it creates all kinds of rules problems, the number one
            // being
            // the ability to use spheroid dropships to perform insta-Death From
            // Above attacks
            // that cannot be defended against
            // So we are going to disallow it
            // http://www.classicbattletech.com/forums/index.php?topic=54112.0
            setLandEnabled(false);
            return;
        }

        final Entity ce = ce();
        if (ce == null) {
            return;
        }

        // only allow landing on the ground map not atmosphere or space map
        if (!clientgui.getClient().getGame().getBoard().onGround()) {
            return;
        }

        if (ce.isAero()) {
            if (ce.isAirborne() && (cmd.getFinalAltitude() == 1)) {
                setLandEnabled(((IAero) ce).canLandHorizontally());
                setVLandEnabled(((IAero) ce).canLandVertically());
            }
        }

    }

    private void updateRollButton() {
        final Entity ce = ce();
        if ((ce == null) || !ce.isAero()) {
            return;
        }

        setRollEnabled(true);

        if (!clientgui.getClient().getGame().getBoard().inSpace()) {
            setRollEnabled(false);
        }

        if (cmd.contains(MoveStepType.ROLL)) {
            setRollEnabled(false);
        }
    }

    private void updateHoverButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }
        
        if (ce.isAero()) {
            if (!((IAero) ce).isVSTOL()) {
                return;
            }
            if (clientgui.getClient().getGame().getBoard().inSpace()) {
                return;
            }
        } else if (!(ce instanceof Protomech) && !(ce instanceof LandAirMech
                && (ce.getConversionMode() == LandAirMech.CONV_MODE_AIRMECH))
                && (ce.getAltitude() <= 3)) {
            return;
        }

        setHoverEnabled(!cmd.contains(MoveStepType.HOVER));
    }

    private void updateThrustButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        // only allow thrust if there is thrust left to spend
        int mpUsed = 0;
        MoveStep last = cmd.getLastStep();
        if (null != last) {
            mpUsed = last.getMpUsed();
        }

        setThrustEnabled(mpUsed < ce.getRunMP());
    }

    private synchronized void updateSpeedButtons() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        IAero a = (IAero) ce;

        // only allow acceleration and deceleration if the cmd is empty or the
        // last step was
        // acc/dec

        setAccEnabled(false);
        setDecEnabled(false);
        MoveStep last = cmd.getLastStep();
        // figure out implied velocity, so you can't decelerate below zero
        int vel = a.getCurrentVelocity();
        int veln = a.getNextVelocity();
        if (null != last) {
            vel = last.getVelocity();
            veln = last.getVelocityN();
        }

        if (null == last) {
            setAccEnabled(true);
            if (vel > 0) {
                setDecEnabled(true);
            }
        } else if (last.getType() == MoveStepType.ACC) {
            setAccEnabled(true);
        } else if ((last.getType() == MoveStepType.DEC) && (vel > 0)) {
            setDecEnabled(true);
        }

        // if the aero has failed a maneuver this turn, then don't allow
        if (a.didFailManeuver()) {
            setAccEnabled(false);
            setDecEnabled(false);
        }

        // if accelerated/decelerate at the end of last turn then disable
        if (a.didAccLast()) {
            setAccEnabled(false);
            setDecEnabled(false);
        }

        // allow acc/dec next if acceleration/deceleration hasn't been used
        setAccNEnabled(false);
        setDecNEnabled(false);
        if (Stream.of(MoveStepType.ACC, MoveStepType.DEC, MoveStepType.DECN)
                .noneMatch(moveStepType -> cmd.contains(moveStepType))) {
            setAccNEnabled(true);
        }

        if (!cmd.contains(MoveStepType.ACC) && !cmd.contains(MoveStepType.DEC)
                && !cmd.contains(MoveStepType.ACCN) && (veln > 0)) {
            setDecNEnabled(true);
        }

        // acc/dec next needs to be disabled if acc/dec used before a failed
        // maneuver
        if (a.didFailManeuver() && a.didAccDecNow()) {
            setDecNEnabled(false);
            setAccNEnabled(false);
        }

        // if in atmosphere, limit acceleration to 2x safe thrust
        if (!clientgui.getClient().getGame().getBoard().inSpace()
                && (vel == (2 * ce.getWalkMP()))) {
            setAccEnabled(false);
        }
        // velocity next will get halved before next turn so allow up to 4 times
        if (!clientgui.getClient().getGame().getBoard().inSpace()
                && (veln == (4 * ce.getWalkMP()))) {
            setAccNEnabled(false);
        }

        // if this is a tele-operated missile then it can't decelerate no matter what
        if (a instanceof TeleMissile) {
            setDecEnabled(false);
            setDecNEnabled(false);
        }
    }

    private void updateFlyOffButton() {
        final Entity ce = ce();

        // Aeros should be able to fly off if they reach a border hex with
        // velocity remaining and facing the right direction
        if ((ce == null) || !ce.isAero() || !ce.isAirborne()) {
            setFlyOffEnabled(false);
            return;
        }

        IAero a = (IAero) ce;
        MoveStep step = cmd.getLastStep();
        Coords position = ce.getPosition();
        int facing = ce.getFacing();

        int velocityLeft = a.getCurrentVelocity();
        if (step != null) {
            position = step.getPosition();
            facing = step.getFacing();
            velocityLeft = step.getVelocityLeft();
        }

        final Board board = clientgui.getClient().getGame().getBoard();
        // for spheroids in atmosphere we just need to check being on the edge
        if (a.isSpheroid() && !board.inSpace()) {
            setFlyOffEnabled((position != null) && (ce.getWalkMP() > 0)
                    && ((position.getX() == 0)
                            || (position.getX() == (board.getWidth() - 1))
                            || (position.getY() == 0)
                            || (position.getY() == (board.getHeight() - 1))));
            return;
        }

        // for all aerodynes and spheroids in space it is more complicated - the
        // nose of the aircraft
        // must be facing in the righ direction and there must be velocity
        // remaining

        boolean evenx = (position.getX() % 2) == 0;
        if ((velocityLeft > 0) && (((position.getX() == 0) && ((facing == 5) || (facing == 4)))
                || ((position.getX() == (board.getWidth() - 1))
                        && ((facing == 1) || (facing == 2)))
                || ((position.getY() == 0) && ((facing == 1) || (facing == 5) || (facing == 0)) && evenx)
                || ((position.getY() == 0) && (facing == 0))
                || ((position.getY() == (board.getHeight() - 1))
                        && ((facing == 2) || (facing == 3) || (facing == 4)) && !evenx)
                || ((position.getY() == (board.getHeight() - 1))
                        && (facing == 3)))) {
            setFlyOffEnabled(true);
        } else {
            setFlyOffEnabled(false);
        }
    }

    private void updateLaunchButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        setLaunchEnabled(!ce.getLaunchableFighters().isEmpty()
                || !ce.getLaunchableSmallCraft().isEmpty()
                || !ce.getLaunchableDropships().isEmpty());
    }

    private void updateDropButton() {
        final Entity ce = ce();
        if (ce == null) {
            return;
        }

        setDropEnabled(ce.isAirborne() && !ce.getDroppableUnits().isEmpty());
    }

    private void updateEvadeButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE)) {
            return;
        }

        if (!((ce instanceof Mech) || (ce instanceof Tank))) {
            return;
        }

        setEvadeEnabled((cmd.getLastStepMovementType() != EntityMovementType.MOVE_JUMP)
                && (cmd.getLastStepMovementType() != EntityMovementType.MOVE_SPRINT));
    }

    private void updateBootleggerButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().getGame().getOptions()
                      .booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
            return;
        }
        
        if (!(ce instanceof Tank || ce instanceof QuadVee)) {
            return;
        }
                
        if (ce.getMovementMode() != EntityMovementMode.WHEELED
                && ce.getMovementMode() != EntityMovementMode.HOVER
                && ce.getMovementMode() != EntityMovementMode.VTOL) {
            return;
        }

        setBootleggerEnabled(cmd.getLastStep() != null && cmd.getLastStep().getNStraight() >= 3);
    }

    private void updateShutdownButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.RPG_MANUAL_SHUTDOWN)) {
            return;
        }

        if (ce instanceof Infantry) {
            return;
        }

        setShutdownEnabled(!ce.isManualShutdown() && !ce.isStartupThisPhase());
    }

    private void updateStartupButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().getGame().getOptions()
                      .booleanOption(OptionsConstants.RPG_MANUAL_SHUTDOWN)) {
            return;
        }

        if (ce instanceof Infantry) {
            return;
        }

        setStartupEnabled(ce.isManualShutdown() && !ce.isShutDownThisPhase());
    }

    private void updateSelfDestructButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().getGame().getOptions()
                      .booleanOption(OptionsConstants.ADVANCED_TACOPS_SELF_DESTRUCT)) {
            return;
        }

        if (ce instanceof Infantry) {
            return;
        }

        setSelfDestructEnabled(ce.hasEngine() && ce.getEngine().isFusion()
                && !ce.getSelfDestructing() && !ce.getSelfDestructInitiated());
    }

    private void updateTraitorButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        setTraitorEnabled(true);
    }
    
    private void updateConvertModeButton() {
        if (cmd.length() > 0 && cmd.getLastStep().getType() != MoveStepType.CONVERT_MODE) {
            setModeConvertEnabled(false);
            return;
        }
        
        final Entity ce = ce();
        
        if (null == ce) {
            setModeConvertEnabled(false);
            return;
        }
        
        if (ce instanceof LandAirMech) {
            boolean canConvert = false;
            for (int i = 0; i < 3; i++) {
                if (i != ce.getConversionMode()
                        && ((LandAirMech) ce).canConvertTo(ce.getConversionMode(), i)) {
                    canConvert = true;
                }
            }
            if (!canConvert) {
                setModeConvertEnabled(false);
                return;
            }
        } else if (!((ce instanceof QuadVee)
                || ((ce instanceof Mech) && ((Mech) ce).hasTracks()))) {
            setModeConvertEnabled(false);
            return;
        }
        
        Hex currHex = clientgui.getClient().getGame().getBoard().getHex(ce.getPosition());
        if (currHex.containsTerrain(Terrains.WATER) && (ce.getElevation() < 0)) {
            setModeConvertEnabled(false);
            return;
        }
        
        if ((ce instanceof LandAirMech) && ce.isGyroDestroyed()) {
            setModeConvertEnabled(false);
            return;
        }
        
        if ((ce instanceof QuadVee) && (((QuadVee) ce).conversionCost() > ce.getRunMP())) {
            setModeConvertEnabled(false);
            return;
        }
        
        setModeConvertEnabled(true);
    }

    private void updateRecklessButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }
        
        if (ce.isAirborne()) {
            setRecklessEnabled(false);
        }

        if (ce instanceof Protomech) {
            setRecklessEnabled(false);
        } else {
            setRecklessEnabled((null == cmd) || (cmd.length() == 0));
        }
    }
    
    private void updateBraceButton() {
        if (null == ce()) {
            return;
        }        
        
        MovePath movePath = cmd;
        if (null == movePath) {
            movePath = new MovePath(this.getClientgui().getClient().getGame(), ce());
        }
        
        if (!movePath.contains(MoveStepType.BRACE) && 
                movePath.isValidPositionForBrace(movePath.getFinalCoords(), movePath.getFinalFacing())) {
            setBraceEnabled(true);
        } else {
            setBraceEnabled(false);
        }
    }

    private void updateManeuverButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (clientgui.getClient().getGame().getBoard().inSpace()) {
            return;
        }

        if (!ce.isAirborne()) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        IAero a = (IAero) ce;

        if (a.isSpheroid()) {
            return;
        }

        if (a.didFailManeuver()) {
            setManeuverEnabled(false);
        }

        if ((null != cmd) && cmd.contains(MoveStepType.MANEUVER)) {
            setManeuverEnabled(false);
        } else {
            setManeuverEnabled(true);
        }
    }
    
    private void updateStrafeButton() {
        if (!clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_VTOL_STRAFING)) {
            return;
        }
        
        setStrafeEnabled(ce() instanceof VTOL);
    }
    
    private void updateBombButton() {
        MoveStep lastStep = cmd.getLastStep();
        if ((lastStep == null)
                && !ce().isAirborneVTOLorWIGE()) {
            setBombEnabled(false);
            return;
        }
        
        if (lastStep != null
                && lastStep.getClearance() <= 0) {
            setBombEnabled(false);
            return;
        }
        
        if (ce().isBomber()
                && ((ce() instanceof LandAirMech)
                        || clientgui.getClient().getGame().getOptions()
                        .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VTOL_ATTACKS))
                && ((IBomber) ce()).getBombPoints() > 0) {
            setBombEnabled(true);
        }
    }
    
    private synchronized void updateLoadButtons() {
        final Game game = clientgui.getClient().getGame();
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (ce instanceof SmallCraft) {
            setUnloadEnabled(!ce.getUnitsUnloadableFromBays().isEmpty() && !ce.isAirborne());
            setLoadEnabled(false);
            setMountEnabled(false);
            return;
        }

        // can this unit mount a dropship/small craft/train?
        setMountEnabled(false);
        Coords pos = ce.getPosition();
        int elev = ce.getElevation();
        int mpUsed = ce.mpUsed;
        if (null != cmd) {
            pos = cmd.getFinalCoords();
            elev = cmd.getFinalElevation();
            mpUsed = cmd.getMpUsed();
        }
        if (!ce.isAirborne()
                && (mpUsed <= Math.ceil((ce.getWalkMP() / 2.0)))
                && game.getBoard().contains(pos)
                && !Compute.getMountableUnits(ce, pos,
                    elev + game.getBoard().getHex(pos).getLevel(), game).isEmpty()) {
            setMountEnabled(true);
        }

        boolean legalGear = ((gear == MovementDisplay.GEAR_LAND)
                             || (gear == MovementDisplay.GEAR_TURN)
                             || (gear == MovementDisplay.GEAR_BACKUP)
                             || (gear == MovementDisplay.GEAR_JUMP));
        int unloadEl = cmd.getFinalElevation();
        Hex hex = ce.getGame().getBoard().getHex(cmd.getFinalCoords());
        
        boolean finalCoordinatesOnBoard = ce.getGame().getBoard().contains(cmd.getFinalCoords());
        boolean canUnloadHere = false;
        
        // if the path's final coordinate are off-board (as could be the case on space maps with advanced movement)
        // we will say that it is not possible to unload units in such a situation.
        if (finalCoordinatesOnBoard) {
            for (Entity en : loadedUnits) {
                if (en.isElevationValid(unloadEl, hex) || (en.getJumpMP() > 0)) {
                    canUnloadHere = true;
                    break;
                }
                // Zip lines, TO pg 219
                if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_ZIPLINES)
                        && (ce() instanceof VTOL) && (en instanceof Infantry) 
                        && !((Infantry) en).isMechanized()) {
                    canUnloadHere = true;
                    break;
                }
            }
        }
        
        // Disable the "Unload" button if we're in the wrong
        // gear or if the entity is not transporting units.
        setUnloadEnabled(legalGear && canUnloadHere && !loadedUnits.isEmpty());

        boolean canDropTrailerHere = false;
        if (finalCoordinatesOnBoard) {
            for (Entity en : towedUnits) {
                if (en.isElevationValid(unloadEl, hex)) {
                    canDropTrailerHere = true;
                    break;
                }
            }
        }
        
        // Disable the "Disconnect" button if we're in the wrong
        // gear or if the entity is not transporting units.
        setDisconnectEnabled(legalGear && canDropTrailerHere && !towedUnits.isEmpty());

        // If the current entity has moved, disable "Load" and "Tow" buttons.
        setLoadEnabled(false);
        setTowEnabled(false);
        if (cen != Entity.NONE) {
            Coords currentPathEndpoint = cmd.getFinalCoords();
            
            // Check the other entities in the current hex for friendly units.
            for (Entity other : game.getEntitiesVector(currentPathEndpoint)) {
                // If the other unit is friendly and not the current entity
                // and the current entity has at least 1 MP, if it can
                // transport the other unit, and if the other hasn't moved
                // then enable the "Load" button. Towing gets handled later,
                // and we don't want both buttons enabled.
                if ((ce.getWalkMP() > 0) && ce.canLoad(other)
                    && other.isLoadableThisTurn() && !ce.canTow(other.getId())) {
                    setLoadEnabled(true);
                    break;
                }
            } // Check the next entity in this position.
            
            // Now check all eligible hexes for towable trailers            
            if (cmd.length() == 0) {
                for (Coords c : ce.getHitchLocations()) {
                    for (Entity other : game.getEntitiesVector(c)) {
                        // If the other unit is friendly and not the current entity
                        // if it can tow the other unit, and if the other hasn't moved
                        // then enable the "Tow" button.
                        if (ce.canTow(other.getId())) {
                            setTowEnabled(true);
                            break;
                        }
                    } // Check the next entity.
                }
            }
        } // End ce-hasn't-moved
    } // private void updateLoadButtons

    private void updateLayMineButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.canLayMine() || cmd.contains(MoveStepType.LAY_MINE)) {
            setLayMineEnabled(false);
        } else if (ce instanceof BattleArmor) {
            setLayMineEnabled(cmd.getLastStep() == null
                || cmd.isJumping()
                || cmd.getLastStepMovementType().equals(EntityMovementType.MOVE_VTOL_WALK));
        } else {
            setLayMineEnabled(true);
        }
    }

    private Entity getMountedUnit() {
        Entity ce = ce();
        Entity choice = null;
        Coords pos = ce.getPosition();
        int elev = ce.getElevation();
        if (null != cmd) {
            pos = cmd.getFinalCoords();
            elev = cmd.getFinalElevation();
        }
        Hex hex = clientgui.getClient().getGame().getBoard().getHex(pos);
        if (null != hex) {
            elev += hex.getLevel();
        }

        List<Entity> mountableUnits = Compute.getMountableUnits(ce, pos, elev, clientgui.getClient().getGame());

        // Handle error condition.
        if (mountableUnits.isEmpty()) {
            LogManager.getLogger().error("Called getMountedUnits without any mountable units.");
        } else if (mountableUnits.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("MovementDisplay.MountUnitDialog.message", ce.getShortName()),
                    Messages.getString("MovementDisplay.MountUnitDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null, SharedUtility.getDisplayArray(mountableUnits),
                    null);
            choice = (Entity) SharedUtility.getTargetPicked(mountableUnits, input);
        } else {
            // Only one choice.
            choice = mountableUnits.get(0);
        }

        if (!(ce instanceof Infantry)) {
            Vector<Integer> bayChoices = new Vector<>();
            for (Transporter t : choice.getTransports()) {
                if (t.canLoad(ce) && (t instanceof Bay)) {
                    bayChoices.add(((Bay) t).getBayNumber());
                }
            }
            String[] retVal = new String[bayChoices.size()];
            int i = 0;
            for (Integer bn : bayChoices) {
                retVal[i++] = bn.toString() + " (Free Slots: "
                              + (int) choice.getBayById(bn).getUnused() + ")";
            }
            if (bayChoices.size() > 1) {
                String bayString = (String) JOptionPane
                        .showInputDialog(
                                clientgui,
                                Messages.getString(
                                        "MovementDisplay.MountUnitBayNumberDialog.message",
                                        new Object[]{choice.getShortName()}),
                                Messages.getString("MovementDisplay.MountUnitBayNumberDialog.title"),
                                JOptionPane.QUESTION_MESSAGE, null, retVal,
                                null);
                ce.setTargetBay(Integer.parseInt(bayString.substring(0,
                                                                     bayString.indexOf(" "))));
                // We need to update the entity here so that the server knows
                // about our target bay
                clientgui.getClient().sendUpdateEntity(ce);
            } else {
                ce.setTargetBay(-1); // Safety set!
            }
        } else {
            ce.setTargetBay(-1); // Safety set!
        }

        // Return the chosen unit.
        return choice;
    }

    private @Nullable Entity getLoadedUnit() {
        final Game game = clientgui.getClient().getGame();
        Entity choice = null;

        Vector<Entity> choices = new Vector<>();
        for (Entity other : game.getEntitiesVector(cmd.getFinalCoords())) {
            if (other.isLoadableThisTurn() && (ce() != null)
                && ce().canLoad(other, false)) {
                choices.addElement(other);
            }
        }

        // Handle error condition.
        if (choices.isEmpty()) {
            LogManager.getLogger().error("getLoadedUnit called without loadable units.");
            return null;
        }

        // If we have multiple choices, display a selection dialog.
        if (choices.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(clientgui,
                                     Messages.getString(
                                             "DeploymentDisplay.loadUnitDialog.message",
                                             new Object[]{ce().getShortName(),
                                                          ce().getUnusedString()}),
                                     Messages.getString("DeploymentDisplay.loadUnitDialog.title"),
                                     JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(choices), null);
            choice = (Entity) SharedUtility.getTargetPicked(choices, input);
        } else {
            // Only one choice.
            choice = choices.get(0);
        }

        if (!(choice instanceof Infantry)) {
            List<Integer> bayChoices = new ArrayList<>();
            for (Transporter t : ce().getTransports()) {
                if (t.canLoad(choice) && (t instanceof Bay)) {
                    bayChoices.add(((Bay) t).getBayNumber());
                }
            }
            if (bayChoices.size() > 1) {
                String[] retVal = new String[bayChoices.size()];
                int i = 0;
                for (Integer bn : bayChoices) {
                    retVal[i++] = bn.toString() + " (Free Slots: "
                            + (int) ce().getBayById(bn).getUnused() + ")";
                }
                String bayString = (String) JOptionPane
                        .showInputDialog(
                                clientgui,
                                Messages.getString(
                                        "MovementDisplay.loadUnitBayNumberDialog.message",
                                        new Object[]{ce().getShortName()}),
                                Messages.getString("MovementDisplay.loadUnitBayNumberDialog.title"),
                                JOptionPane.QUESTION_MESSAGE, null, retVal,
                                null);
                choice.setTargetBay(Integer.parseInt(bayString.substring(0,
                        bayString.indexOf(" "))));
                // We need to update the entity here so that the server knows
                // about our target bay
                clientgui.getClient().sendUpdateEntity(choice);
            } else if (choice.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                bayChoices = new ArrayList<>();
                for (Transporter t : ce().getTransports()) {
                    if ((t instanceof ProtomechClampMount)
                                && t.canLoad(choice)) {
                        bayChoices.add(((ProtomechClampMount) t).isRear() ? 1 : 0);
                    }
                }
                if (bayChoices.size() > 1) {
                    String[] retVal = new String[bayChoices.size()];
                    int i = 0;
                    for (Integer bn : bayChoices) {
                        retVal[i++] = bn > 0?
                                Messages.getString("MovementDisplay.loadProtoClampMountDialog.rear") :
                                    Messages.getString("MovementDisplay.loadProtoClampMountDialog.front");
                    }
                    String bayString = (String) JOptionPane
                            .showInputDialog(
                                    clientgui,
                                    Messages.getString(
                                            "MovementDisplay.loadProtoClampMountDialog.message",
                                            new Object[]{ce().getShortName()}),
                                    Messages.getString("MovementDisplay.loadProtoClampMountDialog.title"),
                                    JOptionPane.QUESTION_MESSAGE, null, retVal,
                                    null);
                    choice.setTargetBay(bayString.equals(Messages
                            .getString("MovementDisplay.loadProtoClampMountDialog.front")) ? 0 : 1);
                    // We need to update the entity here so that the server knows
                    // about our target bay
                    clientgui.getClient().sendUpdateEntity(choice);
                } else {
                    choice.setTargetBay(-1); // Safety set!
                }
            }
        } else {
            choice.setTargetBay(-1); // Safety set!
        }

        // Return the chosen unit.
        return choice;
    }
    
    /**
     * Get the unit (trailer) that the player wants to connect. This method will add the
     * trailer to our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to tow. This
     * value may be null if there are no eligible targets
     */
    private Entity getTowedUnit() {
        final Game game = clientgui.getClient().getGame();
        Entity choice = null;

        List<Entity> choices = new ArrayList<>();
        
        //We have to account for the positions of the whole train when looking to add new trailers
        for (Coords pos : ce().getHitchLocations()) {
            for (Entity other : game.getEntitiesVector(pos)) {
                if (ce() != null && ce().canTow(other.getId())) {
                    choices.add(other);
                }
            }
        }
        
        // Handle error condition.
        if (choices.isEmpty()) {
            LogManager.getLogger().debug("Method called without towable units.");
            return null;
        }

        // If we have multiple choices, display a selection dialog.
        if (choices.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(clientgui,
                                     Messages.getString(
                                             "DeploymentDisplay.towUnitDialog.message",
                                             new Object[]{ce().getShortName()}),
                                     Messages.getString("DeploymentDisplay.towUnitDialog.title"),
                                     JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(choices), null);
            choice = (Entity) SharedUtility.getTargetPicked(choices, input);
        } else {
            // Only one choice.
            choice = choices.get(0);
        }
        
        // Set up the correct hitch/transporter to use
        // We need lots of data about the hitch to store in different places. Save that here
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
                // the string should tell the user if the hitch is mounted front or rear
                if (getHitch().getRearMounted()) {
                    return String.format("%s Trailer Hitch #[%d] (rear)", game.getEntity(id).getShortName(), getNumber());
                }
                return String.format("%s Trailer Hitch #[%d] (front)", game.getEntity(id).getShortName(), getNumber());
            }
        }
        
        // Create a collection to keep my choices in
        List<HitchChoice> hitchChoices = new ArrayList<>();
        
        // next, set up a list of all the entities in this train
        ArrayList<Entity> thisTrain = new ArrayList<>();
        thisTrain.add(ce());
        for (int id : ce().getAllTowedUnits()) {
            Entity tr = game.getEntity(id);
            thisTrain.add(tr);
        }
        // and store all the valid Hitch transporters that each one has
        // really, there shouldn't be but one per entity. "Towing" front and rear with the
        // tractor in the center isn't going to work well...
        for (Entity e : thisTrain) {
            for (Transporter t : e.getTransports()) {
                if (t.canTow(choice)) {
                    TankTrailerHitch h = (TankTrailerHitch) t;
                    HitchChoice hitch = new HitchChoice(e.getId(), e.getTransports().indexOf(t), h);
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
            String selection = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("MovementDisplay.loadUnitHitchDialog.message",
                                    new Object[]{ce().getShortName()}),
                    Messages.getString("MovementDisplay.loadUnitHitchDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null, retVal, null);
            HitchChoice hc = null;
            if (selection != null) {
                for (int loop = 0; loop < hitchChoices.size(); loop++) {
                    if (selection.equals(hitchChoices.get(loop).toString())) {
                        hc = hitchChoices.get(loop);
                        break;
                    }
                }
            }
            // Set the transporter number in the towed entity from the selection
            choice.setTargetBay(hc.getNumber());
            // and then the Entity id the transporter is attached to...
            choice.setTowedBy(hc.getId());
        } else {
            // and in case there's just one choice...
            choice.setTargetBay(hitchChoices.get(0).getNumber());
            choice.setTowedBy(hitchChoices.get(0).getId());
        }

        // We need to update the entities here so that the server knows
        // about our changes
        ce().setTowing(choice.getId());
        clientgui.getClient().sendUpdateEntity(ce());
        clientgui.getClient().sendUpdateEntity(choice);

        // Return the chosen unit.
        return choice;
    }
    
    /**
     * Get the unit that the player wants to unload. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     * value will not be <code>null</code>.
     */
    private Entity getDisconnectedUnit() {
        Entity ce = ce();
        Entity choice = null;
        
        // Handle error condition.
        if (ce.getAllTowedUnits().isEmpty()) {
            LogManager.getLogger().debug("Method called without any towed units.");
            return null;
        } else if (ce.getAllTowedUnits().size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages.getString(
                                    "MovementDisplay.DisconnectUnitDialog.message", new Object[]{
                                                                                             ce.getShortName(), ce.getUnusedString()}),
                            Messages.getString("MovementDisplay.DisconnectUnitDialog.title"),
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(towedUnits), null);
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
     * Get the unit that the player wants to unload. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     * value will not be <code>null</code>.
     */
    private Entity getUnloadedUnit() {
        Entity ce = ce();
        Entity choice = null;
        // Handle error condition.
        if (loadedUnits.isEmpty()) {
            LogManager.getLogger().error("MovementDisplay#getUnloadedUnit() called without loaded units.");
        } else if (loadedUnits.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages.getString(
                                    "MovementDisplay.UnloadUnitDialog.message", new Object[]{
                                                                                             ce.getShortName(), ce.getUnusedString()}),
                            Messages.getString("MovementDisplay.UnloadUnitDialog.title"),
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(loadedUnits), null);
            choice = (Entity) SharedUtility.getTargetPicked(loadedUnits, input);
        } else {
            // Only one choice.
            choice = loadedUnits.get(0);
            loadedUnits.remove(0);
        }

        // Return the chosen unit.
        return choice;
    }

    private Coords getUnloadPosition(Entity unloaded) {
        Entity ce = ce();
        // we need to allow the user to select a hex for offloading
        Coords pos = ce.getPosition();
        if (null != cmd) {
            pos = cmd.getFinalCoords();
        }
        int elev = clientgui.getClient().getGame().getBoard().getHex(pos).getLevel()
                + ce.getElevation();
        List<Coords> ring = pos.allAdjacent();
        if (ce instanceof Dropship) {
            ring = pos.allAtDistance(2);
        }
        // ok now we need to go through the ring and identify available Positions
        ring = Compute.getAcceptableUnloadPositions(ring, unloaded, clientgui.getClient().getGame(), elev);
        //If we're a train, eliminate positions held by any unit in the train. 
        //You get stacking violation weirdness if this isn't done.
        Set<Coords> toRemove = new HashSet<>();
        if (ce.getTowing() != Entity.NONE) {
            for (int i : ce.getAllTowedUnits()) {
                Entity e = ce.getGame().getEntity(i);
                if (e != null && e.getPosition() != null) {
                    toRemove.add(e.getPosition());
                }
            }
        } else if (ce.getTractor() != Entity.NONE) {
            Entity tractor = ce.getGame().getEntity(ce.getTractor());
            if (tractor != null && tractor.getPosition() != null) {
                toRemove.add(tractor.getPosition());
                for (int i : tractor.getAllTowedUnits()) {
                    Entity e = ce.getGame().getEntity(i);
                    if (e != null && e.getPosition() != null) {
                        toRemove.add(e.getPosition());
                    }
                }
            }
        }
        ring.removeAll(toRemove);
        
        if (ring.size() < 1) {
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
        String selected = (String) JOptionPane.showInputDialog(clientgui,
                                                               Messages.getString(
                                                                       "MovementDisplay.ChooseHex" + ".message", new Object[]{
                                                                                                                              ce.getShortName(), ce.getUnusedString()}), Messages
                                                                       .getString("MovementDisplay.ChooseHex.title"),
                                                              
                                                               JOptionPane.QUESTION_MESSAGE, null, choices, null);
        Coords choice = null;
        if (selected == null) {
            return choice;
        }

        for (Coords c : ring) {
            if (selected.equals(c.toString())) {
                choice = c;
                break;
            }
        }
        return choice;
    }
    
    /**
     * Uses player input to find a legal hex where an EjectedCrew unit can be placed
     * @param abandoned - The vessel we're escaping from
     * @return
     */
    private Coords getEjectPosition(Entity abandoned) {
        // we need to allow the user to select a hex for offloading the unit's crew
        Coords pos = abandoned.getPosition();
        // Create a bogus crew entity to use for legal hex calculation
        Entity crew = new EjectedCrew();
        crew.setId(clientgui.getClient().getGame().getNextEntityId());
        crew.setGame(clientgui.getClient().getGame());
        int elev = clientgui.getClient().getGame().getBoard().getHex(pos).getLevel() + abandoned.getElevation();
        List<Coords> ring = pos.allAdjacent();
        if (abandoned instanceof Dropship) {
            ring = pos.allAtDistance(2);
        }
        // ok now we need to go through the ring and identify available Positions
        ring = Compute.getAcceptableUnloadPositions(ring, crew, clientgui.getClient().getGame(), elev);
        if (ring.size() < 1) {
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
        String selected = (String) JOptionPane.showInputDialog(clientgui,
                                                               Messages.getString(
                                                                       "MovementDisplay.ChooseEjectHex.message", new Object[]{
                                                                               abandoned.getShortName(), abandoned.getUnusedString()}), Messages
                                                                       .getString("MovementDisplay.ChooseHex.title"),
                                                              
                                                               JOptionPane.QUESTION_MESSAGE, null, choices, null);
        Coords choice = null;
        if (selected == null) {
            return choice;
        }
        for (Coords c : ring) {
            if (selected.equals(c.toString())) {
                choice = c;
                break;
            }
        }
        return choice;
    }

    /**
     * FIGHTER RECOVERY fighter recovery will be handled differently than
     * loading other units. Namely, it will be an action of the fighter not the
     * carrier. So the fighter just flies right up to a carrier whose movement
     * is ended and hops on. need a new function
     */
    private synchronized void updateRecoveryButton() {
        final Game game = clientgui.getClient().getGame();
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        // I also want to handle fighter recovery here. If using advanced
        // movement
        // it is not a function of where carrier is but where the carrier will
        // be at the end
        // of its move
        if (ce.isAero()) {
            Coords loadeePos = cmd.getFinalCoords();
            if (clientgui.getClient().getGame().useVectorMove()) {
                // not where you are, but where you will be
                loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
            }
            boolean isGood = false;
            for (Entity other : game.getEntitiesVector(loadeePos)) {
                // Is the other unit friendly and not the current entity?
                // must be done with its movement
                // it also must be same heading and velocity
                if ((other instanceof Aero) && other.isDone()
                        && other.canLoad(ce)
                        && (cmd.getFinalFacing() == other.getFacing())
                        && !other.isCapitalFighter()) {
                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (clientgui.getClient().getGame().useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                            if (ce instanceof Dropship) {
                                setDockEnabled(true);
                            } else {
                                setRecoverEnabled(true);
                            }
                            isGood = true;

                            // We can stop looking now.
                            break;
                        }
                    } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                        if (ce instanceof Dropship) {
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
                other = null;
            } // Check the next entity in this position.
            if (!isGood) {
                setRecoverEnabled(false);
                setDockEnabled(false);
            }
        }

    }

    /**
     * Joining a squadron - Similar to fighter recovery. You can fly up and join
     * a squadron or another solo fighter
     */
    private synchronized void updateJoinButton() {
        final Game game = clientgui.getClient().getGame();
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER)) {
            return;
        }

        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isCapitalFighter()) {
            return;
        }

        Coords loadeePos = cmd.getFinalCoords();
        if (game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(),
                                                 cmd.getFinalVectors());
        }
        boolean isGood = false;
        for (Entity other : game.getEntitiesVector(loadeePos)) {
            // Is the other unit friendly and not the current entity?
            // must be done with its movement
            // it also must be same heading and velocity
            if (ce.getOwner().equals(other.getOwner())
                    && other.isCapitalFighter() && other.isDone()
                    && other.canLoad(ce)
                    && (cmd.getFinalFacing() == other.getFacing())) {
                // now lets check velocity
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
            other = null;
        } // Check the next entity in this position.
        if (!isGood) {
            setJoinEnabled(false);
        }
    }

    /**
     * Get the unit that the player wants to unload. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     * value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getLaunchedUnits() {
        Entity ce = ce();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();

        Vector<Entity> launchableFighters = ce.getLaunchableFighters();
        Vector<Entity> launchableSmallCraft = ce.getLaunchableSmallCraft();
        List<Entity> launchableDropships = ce.getLaunchableDropships();

        // Handle error condition.
        if ((launchableFighters.size() <= 0)
                && (launchableSmallCraft.size() <= 0)
                && (launchableDropships.size() <= 0)) {
            LogManager.getLogger().error("MovementDisplay#getLaunchedUnits() called without loaded units.");
            return choices;
        }

        // cycle through the fighter bays and then the small craft bays
        int bayNum = 1;
        int i = 0;
        Bay currentBay;
        int doors = 0;
        Vector<Bay> FighterBays = ce.getFighterBays();
        for (i = 0; i < FighterBays.size(); i++) {
            currentBay = FighterBays.elementAt(i);
            Vector<Integer> bayChoices = new Vector<>();
            Vector<Entity> currentFighters = currentBay.getLaunchableUnits();
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
            String question = Messages
                    .getString(
                            "MovementDisplay.LaunchFighterDialog.message", new Object[]{
                                                                                         ce.getShortName(), doors * 2, bayNum});
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = currentFighters.elementAt(loop).getShortName();
            }

            boolean doIt = false;
            ChoiceDialog choiceDialog = null;
            while (!doIt) {
                choiceDialog = new ChoiceDialog(
                        clientgui.frame,
                        Messages.getString(
                                "MovementDisplay.LaunchFighterDialog.title", new Object[]{
                                                                                           currentBay.getType(), bayNum}), question,
                        names);
                choiceDialog.setVisible(true);
                if (choiceDialog.getChoices() == null) {
                    doIt = true;
                    continue;
                }
                int numChoices = choiceDialog.getChoices().length;
                if ((numChoices > currentBay.getSafeLaunchRate())
                    && GUIPreferences.getInstance().getNagForLaunchDoors()) {
                    int aerosPerDoor = numChoices / doors;
                    int remainder = numChoices % doors;
                    // Determine PSRs
                    StringBuilder psrs = new StringBuilder();
                    for (int choice = 0; choice < numChoices; choice++) {
                        int modifier = aerosPerDoor - 2;
                        if ((choice / aerosPerDoor) >= (doors - 1)) {
                            modifier += remainder;
                        }
                        modifier += currentFighters.get(choice).getCrew().getPiloting();
                        String damageMsg = Messages.getString(
                                "MovementDisplay.LaunchFighterDialog.controlroll", names[choice], modifier);
                        psrs.append("\t" + damageMsg + "\n");
                    }
                    ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                            Messages.getString("MovementDisplay.areYouSure"),
                            Messages.getString("MovementDisplay.ConfirmLaunch") + psrs,
                            true);
                    nag.setVisible(true);
                    doIt = nag.getAnswer();
                    if (!nag.getShowAgain()) {
                        GUIPreferences.getInstance().setNagForLaunchDoors(false);
                    }
                } else {
                    doIt = true;
                }
            }

            if (choiceDialog.getAnswer() && doIt) {
                // load up the choices
                int[] unitsLaunched = choiceDialog.getChoices();
                for (int element : unitsLaunched) {
                    bayChoices.add(currentFighters.elementAt(element).getId());
                    //Prompt the player to load passengers aboard small craft
                    Entity en = clientgui.getClient().getGame().getEntity(currentFighters.elementAt(element).getId());
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
     * Get the unit that the player wants to unload. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     * value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getUndockedUnits() {
        Entity ce = ce();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();

        Vector<Entity> launchableFighters = ce.getLaunchableFighters();
        Vector<Entity> launchableSmallCraft = ce.getLaunchableSmallCraft();
        List<Entity> launchableDropships = ce.getLaunchableDropships();

        // Handle error condition.
        if ((launchableFighters.size() <= 0)
                && (launchableSmallCraft.size() <= 0)
                && (launchableDropships.size() <= 0)) {
            LogManager.getLogger().error("Method called without loaded units.");
        } else {
            // cycle through the docking collars
            int i = 0;
            int collarNum = 1;
            for (DockingCollar collar : ce.getDockingCollars()) {
                List<Entity> currentDropships = collar.getLaunchableUnits();
                Vector<Integer> collarChoices = new Vector<>();
                if (!currentDropships.isEmpty()) {
                    String[] names = new String[currentDropships.size()];
                    String question = Messages.getString("MovementDisplay.LaunchDropshipDialog.message",
                            ce.getShortName(), 1, collarNum);
                    for (int loop = 0; loop < names.length; loop++) {
                        names[loop] = currentDropships.get(loop).getShortName();
                    }

                    boolean doIt = false;
                    ChoiceDialog choiceDialog = new ChoiceDialog(clientgui.frame,
                            Messages.getString("MovementDisplay.LaunchDropshipDialog.title",
                                    collar.getType(), collarNum), question, names);
                    while (!doIt) {
                        choiceDialog = new ChoiceDialog(clientgui.frame,
                                Messages.getString("MovementDisplay.LaunchDropshipDialog.title",
                                        collar.getType(), collarNum), question, names);
                        choiceDialog.setVisible(true);
                        if ((choiceDialog.getChoices() != null)
                                && (choiceDialog.getChoices().length > 1)) {
                            ConfirmDialog nag = new ConfirmDialog(
                                    clientgui.frame,
                                    Messages.getString("MovementDisplay.areYouSure"),
                                    Messages.getString("MovementDisplay.ConfirmLaunch"),
                                    true);
                            nag.setVisible(true);
                            doIt = nag.getAnswer();
                        } else {
                            doIt = true;
                        }
                    }
                    if (choiceDialog.getAnswer() && doIt) {
                        // load up the choices
                        int[] unitsLaunched = choiceDialog.getChoices();
                        for (int element : unitsLaunched) {
                            collarChoices.add(currentDropships.get(element).getId());
                            // Prompt the player to load passengers aboard the launching ship(s)
                            Entity en = clientgui.getClient().getGame().getEntity(currentDropships.get(element).getId());
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
         final Entity currentEntity = ce();
         if (currentEntity == null) {
             LogManager.getLogger().error("Cannot load passenger at launch for a null current entity.");
             return;
         }

         int space = 0;
         for (Bay b : craft.getTransportBays()) {
             if ((b instanceof CargoBay) || (b instanceof InfantryBay) || (b instanceof BattleArmorBay)) {
                 // Assume a passenger takes up 0.1 tons per single infantryman weight calculations
                 space += (int) Math.round(b.getUnused() / 0.1);
             }
         }
         // Passengers don't actually 'load' into bays to consume space, so update what's available for anyone
         // already aboard
         space -= ((craft.getTotalOtherCrew() + craft.getTotalPassengers()) * 0.1);
         // Make sure the text displays either the carrying capacity or the number of passengers left aboard
         space = Math.min(space, currentEntity.getNPassenger());
         ConfirmDialog takePassenger = new ConfirmDialog(clientgui.frame,
                 Messages.getString("MovementDisplay.FillSmallCraftPassengerDialog.Title"),
                 Messages.getString("MovementDisplay.FillSmallCraftPassengerDialog.message",
                         craft.getShortName(), space, currentEntity.getShortName() + "'",
                         currentEntity.getNPassenger()), false);
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

    /**
     * Get the unit that the player wants to drop. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     * value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getDroppedUnits() {
        Entity ce = ce();
        if (ce == null) {
            LogManager.getLogger().error("Cannot get dropped units for a null current entity");
            return new TreeMap<>();
        }

        Vector<Entity> droppableUnits = ce.getDroppableUnits();

        if (droppableUnits.isEmpty()) {
            LogManager.getLogger().error("Cannot get dropped units when no units are droppable.");
            return new TreeMap<>();
        }

        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();
        Set<Integer> alreadyDropped = cmd.getDroppedUnits();
        // cycle through the bays
        int bayNum = 1;
        Vector<Bay> Bays = ce.getTransportBays();
        for (int i = 0; i < Bays.size(); i++) {
            Bay currentBay = Bays.elementAt(i);
            Vector<Integer> bayChoices = new Vector<>();
            List<Entity> currentUnits = currentBay.getDroppableUnits().stream()
                    .filter(e -> !alreadyDropped.contains(e.getId()))
                    .collect(Collectors.toList());

            int doors = currentBay.getCurrentDoors();
            if (!currentUnits.isEmpty() && (doors > 0)) {
                String[] names = new String[currentUnits.size()];
                String question = Messages.getString("MovementDisplay.DropUnitDialog.message",
                        doors, bayNum);
                for (int loop = 0; loop < names.length; loop++) {
                    names[loop] = currentUnits.get(loop).getShortName();
                }
                ChoiceDialog choiceDialog = new ChoiceDialog(clientgui.frame,
                        Messages.getString("MovementDisplay.DropUnitDialog.title",
                                currentBay.getType(), bayNum), question, names, false, doors);
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
        final Game game = clientgui.getClient().getGame();
        final Entity ce = ce();
        List<Entity> choices = new ArrayList<>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (clientgui.getClient().getGame().useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
        }
        for (Entity other : game.getEntitiesVector(loadeePos)) {
            // Is the other unit friendly and not the current entity?
            // must be done with its movement
            // it also must be same heading and velocity
            if ((other instanceof Aero) && !((Aero) other).isOutControlTotal()
                    && other.isDone() && other.canLoad(ce)
                    && ce.isLoadableThisTurn()
                    && (cmd.getFinalFacing() == other.getFacing())) {
                // now lets check velocity
                // depends on movement rules
                Aero oa = (Aero) other;
                if (clientgui.getClient().getGame().useVectorMove()) {
                    if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                        choices.add(other);
                    }
                } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                    choices.add(other);
                }
            }
            // Nope. Discard it.
            other = null;
        }

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            if (choices.get(0).mpUsed > 0) {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.RecoverSureDialog.title"),
                        Messages.getString("MovementDisplay.RecoverSureDialog.message"))) {
                    return choices.get(0).getId();
                }
            } else {
                return choices.get(0).getId();
            }
            return -1;
        }

        String input = (String) JOptionPane.showInputDialog(clientgui,
                Messages.getString("MovementDisplay.RecoverFighterDialog.message"),
                Messages.getString("MovementDisplay.RecoverFighterDialog.title"),
                JOptionPane.QUESTION_MESSAGE, null, SharedUtility.getDisplayArray(choices),
                null);
        Entity picked = (Entity) SharedUtility.getTargetPicked(choices, input);

        if (picked != null) {
            // if this unit is thrusting, make sure they are aware
            if (picked.mpUsed > 0) {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.RecoverSureDialog.title"),
                        Messages.getString("MovementDisplay.RecoverSureDialog.message"))) {
                    return picked.getId();
                }
            } else {
                return picked.getId();
            }
        }
        return -1;
    }

    /**
     * @return the unit id that the player wants to join
     */
    private int getUnitJoined() {
        final Game game = clientgui.getClient().getGame();
        final Entity ce = ce();
        List<Entity> choices = new ArrayList<>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (clientgui.getClient().getGame().useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
        }
        for (Entity other : game.getEntitiesVector(loadeePos)) {
            // Is the other unit friendly and not the current entity?
            // must be done with its movement
            // it also must be same heading and velocity
            if ((other instanceof Aero) && !((Aero) other).isOutControlTotal()
                    && other.isDone() && other.canLoad(ce)
                    && ce.isLoadableThisTurn()
                    && (cmd.getFinalFacing() == other.getFacing())) {
                // now lets check velocity
                // depends on movement rules
                Aero oa = (Aero) other;
                if (clientgui.getClient().getGame().useVectorMove()) {
                    if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                        choices.add(other);
                    }
                } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                    choices.add(other);
                }
            }
            // Nope. Discard it.
            other = null;
        }

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            return choices.get(0).getId();
        }

        String input = (String) JOptionPane.showInputDialog(
                clientgui,
                Messages.getString("MovementDisplay.JoinSquadronDialog.message"),
                Messages.getString("MovementDisplay.JoinSquadronDialog.title"),
                JOptionPane.QUESTION_MESSAGE, null,
                SharedUtility.getDisplayArray(choices), null);
        Entity picked = (Entity) SharedUtility.getTargetPicked(choices, input);
        if (picked != null) {
            return picked.getId();
        }
        return -1;
    }

    /**
     * check for out of control and adjust buttons
     */
    private void checkOOC() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        IAero a = (IAero) ce;

        if (a.isOutControlTotal() && a.isAirborne()) {
            disableButtons();
            butDone.setEnabled(true);
            if (numButtonGroups > 1) {
                getBtn(MoveCommand.MOVE_MORE).setEnabled(true);
            }
            getBtn(MoveCommand.MOVE_NEXT).setEnabled(true);
            setForwardIniEnabled(true);
            if (ce instanceof Aero) {
                setLaunchEnabled(!ce.getLaunchableFighters().isEmpty()
                        || !ce.getLaunchableSmallCraft().isEmpty()
                        || !ce.getLaunchableDropships().isEmpty());
            }
        }
    }

    /**
     * check for fuel and adjust buttons
     */
    private void checkFuel() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        IAero a = (IAero) ce;
        if (a.getCurrentFuel() < 1) {
            disableButtons();
            butDone.setEnabled(true);
            getBtn(MoveCommand.MOVE_NEXT).setEnabled(true);
            setForwardIniEnabled(true);
            if (ce instanceof Aero) {
                setLaunchEnabled(!ce.getLaunchableFighters().isEmpty()
                        || !ce.getLaunchableSmallCraft().isEmpty()
                        || !ce.getLaunchableDropships().isEmpty());
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
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!ce.isAero()) {
            return;
        }

        IAero a = (IAero) ce;
        if (!clientgui.getClient().getGame().getBoard().inSpace()) {
            if (a.isSpheroid()
                    || clientgui.getClient().getGame().getPlanetaryConditions().isVacuum()) {
                getBtn(MoveCommand.MOVE_ACC).setEnabled(false);
                getBtn(MoveCommand.MOVE_DEC).setEnabled(false);
                getBtn(MoveCommand.MOVE_ACCN).setEnabled(false);
                getBtn(MoveCommand.MOVE_DECN).setEnabled(false);
            }
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Game game = clientgui.getClient().getGame();
        final Entity ce = ce();

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.

        // Convert the choices into a List of targets.
        ArrayList<Targetable> targets = new ArrayList<>();
        for (Entity ent : game.getEntitiesVector(pos)) {
            if ((ce == null) || !ce.equals(ent)) {
                targets.add(ent);
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().getGame().getBoard().getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame().getBoard(), false));
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("MovementDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                    Messages.getString("MovementDisplay.ChooseTargetDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null, SharedUtility.getDisplayArray(targets),
                    null);
            choice = SharedUtility.getTargetPicked(targets, input);
        }

        return choice;
    }

    private int chooseMineToLay() {
        MineLayingDialog mld = new MineLayingDialog(clientgui.frame, ce());
        mld.setVisible(true);
        return mld.getAnswer() ? mld.getMine() : -1;
    }

    private void dumpBombs() {
        if (!ce().isAero()) {
            return;
        }

        EntityMovementType overallMoveType = EntityMovementType.MOVE_NONE;
        if (null != cmd) {
            overallMoveType = cmd.getLastStepMovementType();
        }
        // bring up dialog to dump bombs, then make a control roll and report
        // success or failure
        // should update mp available
        int numFighters = ce().getActiveSubEntities().size();
        BombPayloadDialog dumpBombsDialog = new BombPayloadDialog(
                clientgui.frame,
                Messages.getString("MovementDisplay.BombDumpDialog.title"),
                ce().getBombLoadout(), false, true, -1, numFighters);
        dumpBombsDialog.setVisible(true);
        if (dumpBombsDialog.getAnswer()) {
            // int[] bombsDumped =
            dumpBombsDialog.getChoices();
            // first make a control roll
            PilotingRollData psr = ce().getBasePilotingRoll(overallMoveType);
            int ctrlroll = Compute.d6(2);
            Report r = new Report(9500);
            r.subject = ce().getId();
            r.add(ce().getDisplayName());
            r.add(psr);
            r.add(ctrlroll);
            r.newlines = 0;
            r.indent(1);
            if (ctrlroll < psr.getValue()) {
                r.choose(false);
                String title = Messages.getString("MovementDisplay.DumpingBombs.title");
                String body = Messages.getString("MovementDisplay.DumpFailure.message");
                clientgui.doAlertDialog(title, body);
                // failed the roll, so dump all bombs
                ce().getBombLoadout();
            } else {
                // avoided damage
                r.choose(true);
                String title = Messages
                        .getString("MovementDisplay.DumpingBombs.title");
                String body = Messages
                        .getString("MovementDisplay.DumpSuccessful.message");
                clientgui.doAlertDialog(title, body);
            }
        }

    }

    /**
     * based on maneuver type add the appropriate steps return true if we should
     * redraw the movement data
     */
    private boolean addManeuver(int type) {
        cmd.addManeuver(type);
        switch (type) {
            case ManeuverType.MAN_HAMMERHEAD:
                cmd.addStep(MoveStepType.YAW, true, true);
                return true;
            case ManeuverType.MAN_HALF_ROLL:
                cmd.addStep(MoveStepType.ROLL, true, true);
                return true;
            case ManeuverType.MAN_BARREL_ROLL:
                cmd.addStep(MoveStepType.DEC, true, true);
                return true;
            case ManeuverType.MAN_IMMELMAN:
                gear = MovementDisplay.GEAR_IMMEL;
                return false;
            case ManeuverType.MAN_SPLIT_S:
                gear = MovementDisplay.GEAR_SPLIT_S;
                return false;
            case ManeuverType.MAN_VIFF:
                if (!ce().isAero()) {
                    return false;
                }
                IAero a = (IAero) ce();
                MoveStep last = cmd.getLastStep();
                int vel = a.getCurrentVelocity();
                if (null != last) {
                    vel = last.getVelocityLeft();
                }
                while (vel > 0) {
                    cmd.addStep(MoveStepType.DEC, true, true);
                    vel--;
                }
                cmd.addStep(MoveStepType.UP);
                return true;
            case ManeuverType.MAN_SIDE_SLIP_LEFT:
                // If we are on a ground map, slide slip works slightly differently
                // See Total Warfare pg 85
                if (clientgui.getClient().getGame().getBoard().getType() == Board.T_GROUND) {
                    for (int i = 0; i < 8; i++) {
                        cmd.addStep(MoveStepType.LATERAL_LEFT, true, true);
                    }
                    for (int i = 0; i < 8; i++) {
                        cmd.addStep(MoveStepType.FORWARDS, true, true);
                    }
                } else {
                    cmd.addStep(MoveStepType.LATERAL_LEFT, true, true);
                }
                return true;
            case ManeuverType.MAN_SIDE_SLIP_RIGHT:
                // If we are on a ground map, slide slip works slightly differently
                // See Total Warfare pg 85
                if (clientgui.getClient().getGame().getBoard().getType() == Board.T_GROUND) {
                    for (int i = 0; i < 8; i++) {
                        cmd.addStep(MoveStepType.LATERAL_RIGHT, true, true);
                    }
                    for (int i = 0; i < 8; i++) {
                        cmd.addStep(MoveStepType.FORWARDS, true, true);
                    }
                } else {
                    cmd.addStep(MoveStepType.LATERAL_RIGHT, true, true);
                }
                return true;
            case ManeuverType.MAN_LOOP:
                cmd.addStep(MoveStepType.LOOP, true, true);
                return true;
            default:
                return false;
        }
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
        // On simultaneous phases, each player ending their turn will generalte a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (clientgui.getClient().getGame().getPhase().isSimultaneous(clientgui.getClient().getGame())
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (clientgui.getClient().getGame().getTurnIndex() != 0)) {
            return;
        }
        
        // if all our entities are actually done, don't start up the turn.
        if (clientgui.getClient().getGame().getPlayerEntities(clientgui.getClient().getLocalPlayer(), false)
                .stream().allMatch(Entity::isDone)) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase() != GamePhase.MOVEMENT) {
            // ignore
            return;
        }

        if (clientgui.getClient().isMyTurn()) {
            // Can the player unload entities stranded on immobile transports?
            if (clientgui.getClient().canUnloadStranded()) {
                unloadStranded();
            } else if (cen == Entity.NONE) {
                beginMyTurn();
            }
        } else {
            endMyTurn();
            if ((e.getPlayer() == null)
                    && (clientgui.getClient().getGame().getTurn() instanceof UnloadStrandedTurn)) {
                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother"));
            } else {
                String playerName;
                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }
                setStatusBarText(Messages.getString("MovementDisplay.its_others_turn", playerName));
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            endMyTurn();
        }
        
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && !clientgui.getClient().getGame().getPhase().isMovement()) {
            endMyTurn();
        }

        if (clientgui.getClient().getGame().getPhase().isMovement()) {
            setStatusBarText(Messages.getString("MovementDisplay.waitingForMovementPhase"));
        }
    }
    
    /**
     * Computes all of the possible moves for an Entity in a particular gear. The Entity can either
     * be a suggested Entity or the currently selected one. If there is a selected entity (which
     * implies it's the current players turn), then the current gear is used (which is set by the
     * user). If there is no selected entity, then the current gear is invalid, and it defaults to
     * GEAR_LAND (standard "walk forward").
     * 
     * @param suggestion The suggested Entity to use to compute the movement envelope. If used, the
     *                   gear will be set to GEAR_LAND. This takes precendence over the currently
     *                   selected unit.
     * @param suggestion
     */
    public void computeMovementEnvelope(Entity suggestion) {
        // do nothing if deactivated in the settings
        if (!GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.MOVE_ENVELOPE)) {
            clientgui.getBoardView().clearMovementEnvelope();
            return;
        }
        
        Entity en = ce();
        int mvMode = gear;
        if ((en == null) && (suggestion == null)) {
            return;
        } else if (en == null) {
            en = suggestion;
            mvMode = GEAR_LAND;
        } else {
            en = suggestion;
        }
        if (en.isDone()) {
            return;
        }
        
        Map<Coords, MovePath> mvEnvData = new HashMap<>();
        MovePath mp = new MovePath(clientgui.getClient().getGame(), en);

        int maxMP;
        if (mvMode == GEAR_JUMP || mvMode == GEAR_DFA) {
            maxMP = en.getJumpMP();
        } else if (mvMode == GEAR_BACKUP) {
            maxMP = en.getWalkMP();
        } else if ((ce() instanceof Mech) && !(ce() instanceof QuadVee)
                && (ce().getMovementMode() == EntityMovementMode.TRACKED)) {
            // A non-QuadVee 'Mech that is using tracked movement is limited to walking
            maxMP = en.getWalkMP();
        } else {
            if (clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT)) {
                maxMP = en.getSprintMP();
            } else {
                maxMP = en.getRunMP();
            }
        }
        MoveStepType stepType = (mvMode == GEAR_BACKUP) ? MoveStepType.BACKWARDS
                : MoveStepType.FORWARDS;
        if (mvMode == GEAR_JUMP || mvMode == GEAR_DFA) {
            mp.addStep(MoveStepType.START_JUMP);
        }

        ShortestPathFinder pf = ShortestPathFinder.newInstanceOfOneToAll(maxMP, stepType, en.getGame());
        pf.run(mp);
        mvEnvData = pf.getAllComputedPaths();
        Map<Coords, Integer> mvEnvMP = new HashMap<>((int) ((mvEnvData.size() * 1.25) + 1));
        for (Coords c : mvEnvData.keySet()) {
            mvEnvMP.put(c, mvEnvData.get(c).countMp(mvMode == GEAR_JUMP));
        }
        clientgui.getBoardView().setMovementEnvelope(mvEnvMP, en.getWalkMP(), en
                .getRunMP(), en.getJumpMP(), mvMode);
    }

    public void computeModifierEnvelope() {
        if (ce() == null) {
            return;
        }
        int maxMP;
        if (gear == GEAR_JUMP) {
            maxMP = ce().getJumpMP();
        } else if (gear == GEAR_BACKUP) {
            maxMP = ce().getWalkMP();
        } else if (ce() instanceof Mech && !(ce() instanceof QuadVee)
                && ce().getMovementMode() == EntityMovementMode.TRACKED) {
            // A non-QuadVee 'Mech that is using tracked movement (or converting to it) is limited to walking
            maxMP = ce().getWalkMP();
        } else {
            maxMP = ce().getRunMP();
        }
        MoveStepType stepType = (gear == GEAR_BACKUP) ? MoveStepType.BACKWARDS : MoveStepType.FORWARDS;
        MovePath mp = new MovePath(clientgui.getClient().getGame(), ce());
        if (gear == GEAR_JUMP) {
            mp.addStep(MoveStepType.START_JUMP);
        }
        LongestPathFinder lpf = LongestPathFinder.newInstanceOfLongestPath(maxMP, stepType, ce().getGame());
        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
        AbstractPathFinder.StopConditionTimeout<MovePath> timeoutCondition = new AbstractPathFinder.StopConditionTimeout<>(
                timeLimit * 10);
        lpf.addStopCondition(timeoutCondition);
        lpf.run(mp);
        clientgui.getBoardView().setMovementModifierEnvelope(lpf.getLongestComputedPaths());
    }

    //
    // ActionListener
    //
    @Override
    public synchronized void actionPerformed(ActionEvent ev) {
        final Entity ce = ce();

        if (ce == null) {
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
        final String actionCmd = ev.getActionCommand();
        final AbstractOptions opts = clientgui.getClient().getGame().getOptions();
        if (actionCmd.equals(MoveCommand.MOVE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (actionCmd.equals(
                MoveCommand.MOVE_FORWARD_INI.getCmd())) {
            selectNextPlayer();
        } else if (actionCmd.equals(MoveCommand.MOVE_CANCEL.getCmd())) {
            clear();
            computeMovementEnvelope(ce);
        } else if (ev.getSource().equals(getBtn(MoveCommand.MOVE_MORE))) {
            currentButtonGroup++;
            currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        } else if (actionCmd.equals(MoveCommand.MOVE_UNJAM.getCmd())) {
            String title = Messages.getString("MovementDisplay.UnjamRAC.title");
            String msg = Messages.getString("MovementDisplay.UnjamRAC.message");
            if ((gear == MovementDisplay.GEAR_JUMP)
                    || (gear == MovementDisplay.GEAR_CHARGE)
                    || (gear == MovementDisplay.GEAR_DFA)
                    || ((cmd.getMpUsed() > ce.getWalkMP()) 
                            && !(cmd.getLastStep().isOnlyPavement() 
                                    && (cmd.getMpUsed() <= (ce.getWalkMP() + 1))))
                    || (opts.booleanOption("tacops_tank_crews")
                            && (cmd.getMpUsed() > 0) && (ce instanceof Tank) 
                            && (ce.getCrew().getSize() < 2))
                    || (gear == MovementDisplay.GEAR_SWIM)
                    || (gear == MovementDisplay.GEAR_RAM)) {
                // in the wrong gear
                // clearAllMoves();
                // gear = Compute.GEAR_LAND;
                setUnjamEnabled(false);
            } else  if (clientgui.doYesNoDialog(title, msg)) {
                cmd.addStep(MoveStepType.UNJAM_RAC);
                ready();
                // If ready() fires, it will call endMyTurn, which sets cen to
                // Entity.NONE. If this doesn't happen, it means that the
                // ready() was cancelled (ie, not all Velocity is spent), if it
                // is cancelled we have to ensure the UNJAM_RAC step is removed,
                // otherwise it can fire multiple times.
                if (cen != Entity.NONE) {
                    cmd.removeLastStep();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_SEARCHLIGHT.getCmd())) {
            cmd.addStep(MoveStepType.SEARCHLIGHT);
        } else if (actionCmd.equals(MoveCommand.MOVE_WALK.getCmd())) {
            if ((gear == MovementDisplay.GEAR_JUMP) || (gear == MovementDisplay.GEAR_SWIM)) {
                gear = MovementDisplay.GEAR_LAND;
                clear();
            }
            Color walkColor = GUIPreferences.getInstance().getColor(GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
            clientgui.getBoardView().setHighlightColor(walkColor);
            gear = MovementDisplay.GEAR_LAND;
            computeMovementEnvelope(ce);
        } else if (actionCmd.equals(MoveCommand.MOVE_JUMP.getCmd())) {
            if ((gear != MovementDisplay.GEAR_JUMP)
                    && !((cmd.getLastStep() != null)
                            && cmd.getLastStep().isFirstStep() 
                            && (cmd.getLastStep().getType() == MoveStepType.LAY_MINE))) {
                clear();
            }
            if (!cmd.isJumping()) {
                cmd.addStep(MoveStepType.START_JUMP);
            }
            gear = MovementDisplay.GEAR_JUMP;
            Color jumpColor = GUIPreferences.getInstance().getColor(GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
            clientgui.getBoardView().setHighlightColor(jumpColor);
            computeMovementEnvelope(ce);
        } else if (actionCmd.equals(MoveCommand.MOVE_SWIM.getCmd())) {
            if (gear != MovementDisplay.GEAR_SWIM) {
                clear();
            }
            // dcmd.addStep(MoveStepType.SWIM);
            gear = MovementDisplay.GEAR_SWIM;
            ce.setMovementMode((ce instanceof BipedMech) ? EntityMovementMode.BIPED_SWIM
                    : EntityMovementMode.QUAD_SWIM);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_CONVERT.getCmd())) {
            EntityMovementMode nextMode = ce.nextConversionMode(cmd.getFinalConversionMode());
            // LAMs may have to skip the next mode due to damage
            if (ce instanceof LandAirMech) {
                if (!((LandAirMech) ce).canConvertTo(nextMode)) {
                    nextMode = ce.nextConversionMode(nextMode);
                }
                if (!((LandAirMech) ce).canConvertTo(nextMode)) {
                    nextMode = ce.getMovementMode();
                }
            }
            adjustConvertSteps(nextMode);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_LEG.getCmd())) {
            if ((ce.getEntityType() & Entity.ETYPE_QUAD_MECH) == Entity.ETYPE_QUAD_MECH) {
                adjustConvertSteps(EntityMovementMode.QUAD);                
            } else if ((ce.getEntityType() & Entity.ETYPE_TRIPOD_MECH) == Entity.ETYPE_TRIPOD_MECH) {
                adjustConvertSteps(EntityMovementMode.TRIPOD);
            } else if ((ce.getEntityType() & Entity.ETYPE_BIPED_MECH) == Entity.ETYPE_BIPED_MECH) {
                adjustConvertSteps(EntityMovementMode.BIPED);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_VEE.getCmd())) {
            if (ce instanceof QuadVee && ((QuadVee) ce).getMotiveType() == QuadVee.MOTIVE_WHEEL) {
                adjustConvertSteps(EntityMovementMode.WHEELED);
            } else if ((ce instanceof Mech && ((Mech) ce).hasTracks())
                    || ce instanceof QuadVee) {
                adjustConvertSteps(EntityMovementMode.TRACKED);
            } else if (ce instanceof LandAirMech
                    && ((LandAirMech) ce).getLAMType() == LandAirMech.LAM_STANDARD) {
                adjustConvertSteps(EntityMovementMode.WIGE);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_AIR.getCmd())) {
            if (ce instanceof LandAirMech) {
                adjustConvertSteps(EntityMovementMode.AERODYNE);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN.getCmd())) {
            gear = MovementDisplay.GEAR_TURN;
        } else if (actionCmd.equals(MoveCommand.MOVE_BACK_UP.getCmd())) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                gear = MovementDisplay.GEAR_BACKUP; // on purpose...
                clear();
            }
            gear = MovementDisplay.GEAR_BACKUP; // on purpose...
            Color backColor = GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_MOVE_BACK_COLOR);
            clientgui.getBoardView().setHighlightColor(backColor);
            computeMovementEnvelope(ce);
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
            if (!clientgui.getClient().getGame()
                    .containsMinefield(ce.getPosition())) {
                clientgui.doAlertDialog(Messages
                        .getString("MovementDisplay.CantClearMinefield"),
                        Messages.getString("MovementDisplay.NoMinefield"));
                return;
            }

            // Does the entity has a minesweeper?
            int clear = Minefield.CLEAR_NUMBER_INFANTRY;
            int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;
            // Check for Minesweeping Engineers
            if ((ce() instanceof Infantry)) {
                Infantry inf = (Infantry) ce();
                if (inf.hasSpecialization(Infantry.MINE_ENGINEERS)) {
                    clear = Minefield.CLEAR_NUMBER_INF_ENG;
                    boom = Minefield.CLEAR_NUMBER_INF_ENG_ACCIDENT;
                }
            }
            // Check for Mine clearance manipulators on BA
            if ((ce() instanceof BattleArmor)) {
                BattleArmor ba = (BattleArmor) ce();
                String mcmName = BattleArmor.MANIPULATOR_TYPE_STRINGS
                        [BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE];
                if (ba.getLeftManipulatorName().equals(mcmName)) {
                    clear = Minefield.CLEAR_NUMBER_BA_SWEEPER;
                    boom = Minefield.CLEAR_NUMBER_BA_SWEEPER_ACCIDENT;
                }
            }

            // need to choose a mine
            List<Minefield> mfs = clientgui.getClient().getGame().getMinefields(ce.getPosition());
            String[] choices = new String[mfs.size()];
            for (int loop = 0; loop < choices.length; loop++) {
                choices[loop] = Minefield.getDisplayableName(mfs.get(loop).getType());
            }
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("MovementDisplay.ChooseMinefieldDialog.message"),
                    Messages.getString("MovementDisplay.ChooseMinefieldDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null, choices, null);
            Minefield mf = null;
            if (input != null) {
                for (int loop = 0; loop < choices.length; loop++) {
                    if (input.equals(choices[loop])) {
                        mf = mfs.get(loop);
                        break;
                    }
                }
            }
            String title = Messages.getString("MovementDisplay.ClearMinefieldDialog.title");
            String msg = Messages.getString("MovementDisplay.ClearMinefieldDialog.message", clear, boom);
            if ((null != mf) && clientgui.doYesNoDialog(title, msg)) {
                cmd.addStep(MoveStepType.CLEAR_MINEFIELD, mf);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_CHARGE.getCmd())) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            gear = MovementDisplay.GEAR_CHARGE;
            computeMovementEnvelope(ce);
        } else if (actionCmd.equals(MoveCommand.MOVE_DFA.getCmd())) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_DFA;
            computeMovementEnvelope(ce);
            if (!cmd.isJumping()) {
                cmd.addStep(MoveStepType.START_JUMP);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_RAM.getCmd())) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            if (ce.isAirborneVTOLorWIGE()) {
                gear = MovementDisplay.GEAR_CHARGE;
            } else {
                gear = MovementDisplay.GEAR_RAM;
            }
            computeMovementEnvelope(ce);
        } else if (actionCmd.equals(MoveCommand.MOVE_GET_UP.getCmd())) {
            // if the unit has a hull down step
            // then don't clear the moves
            if (!cmd.contains(MoveStepType.HULL_DOWN)) {
                clear();
            }

            if (opts.booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND)
                    && (ce.getWalkMP() > 2)) {
                ConfirmDialog response = clientgui.doYesNoBotherDialog(
                        Messages.getString("MovementDisplay.CarefulStand.title"),
                        Messages.getString("MovementDisplay.CarefulStand.message"));
                if (response.getAnswer()) {
                    ce.setCarefulStand(true);
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        cmd.addStep(MoveStepType.CAREFUL_STAND);
                    }
                } else {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        cmd.addStep(MoveStepType.GET_UP);
                    }
                }
            } else {
                butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Move") + "</b></html");
                if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                    cmd.addStep(MoveStepType.GET_UP);
                }
            }

            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_GO_PRONE.getCmd())) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                cmd.addStep(MoveStepType.GO_PRONE);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
            butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Move") + "</b></html>");
        } else if (actionCmd.equals(MoveCommand.MOVE_HULL_DOWN.getCmd())) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalHullDown()) {
                cmd.addStep(MoveStepType.HULL_DOWN);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
            butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Move") + "</b></html>");
        } else if (actionCmd.equals(MoveCommand.MOVE_BRACE.getCmd())) {
            var options = ce().getValidBraceLocations();
            if (options.size() == 1) {
                cmd.addStep(MoveStepType.BRACE, options.get(0));
                butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Done") + "</b></html>");
            } else if (options.size() > 1) {
                String[] locationNames = new String[options.size()];
                
                for (int x = 0; x < options.size(); x++) {
                    locationNames[x] = ce().getLocationName(options.get(x));
                }
                
                // Dialog for choosing which location to brace
                String option = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                                "Choose the location to brace:",
                                "Choose Brace Location", JOptionPane.QUESTION_MESSAGE, null,
                                locationNames, locationNames[0]);
    
                // Verify that we have a valid option...
                if (option != null) {
                    int id = options.get(Arrays.asList(locationNames).indexOf(option));
                    cmd.addStep(MoveStepType.BRACE, id);
                    butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Done") + "</b></html>");
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_FLEE.getCmd())
                && clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.EscapeDialog.title"),
                        Messages.getString("MovementDisplay.EscapeDialog.message"))) {
           
            clear();
            cmd.addStep(MoveStepType.FLEE);
            ready();
        } else if (actionCmd.equals(MoveCommand.MOVE_FLY_OFF.getCmd())
                && clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.FlyOffDialog.title"),
                        Messages.getString("MovementDisplay.FlyOffDialog.message"))) {
            if (opts.booleanOption(OptionsConstants.ADVAERORULES_RETURN_FLYOVER)
                    && clientgui.doYesNoDialog(
                            Messages.getString("MovementDisplay.ReturnFly.title"),
                            Messages.getString("MovementDisplay.ReturnFly.message"))) {
                cmd.addStep(MoveStepType.RETURN);
            } else {
                cmd.addStep(MoveStepType.OFF);
            }
            ready();
        } else if (actionCmd.equals(MoveCommand.MOVE_EJECT.getCmd())) {
            if (ce instanceof Tank) {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.AbandonDialog.title"),
                        Messages.getString("MovementDisplay.AbandonDialog.message"))) {
                    clear();
                    cmd.addStep(MoveStepType.EJECT);
                    ready();
                }
            } else if (ce.isLargeCraft()) {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.AbandonDialog.title"),
                        Messages.getString("MovementDisplay.AbandonDialog.message"))) {
                    clear();
                    // If we're abandoning while grounded, find a legal position to put an EjectedCrew unit
                    if (!ce.isSpaceborne() && ce.getAltitude() == 0) {
                        Coords pos = getEjectPosition(ce);
                        cmd.addStep(MoveStepType.EJECT, ce, pos);
                    } else {
                        cmd.addStep(MoveStepType.EJECT);
                    }
                    ready();
                }
            } else if (clientgui
                    .doYesNoDialog(
                            Messages.getString("MovementDisplay.AbandonDialog1.title"),
                            Messages.getString("MovementDisplay.AbandonDialog1.message"))) {
                clear();
                cmd.addStep(MoveStepType.EJECT);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_LOAD.getCmd())) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = getLoadedUnit();
            if (other != null) {
                cmd.addStep(MoveStepType.LOAD);
                clientgui.getBoardView().drawMovementData(ce(), cmd);
                gear = MovementDisplay.GEAR_LAND;
            } // else - didn't find a unit to load
        } else if (actionCmd.equals(MoveCommand.MOVE_TOW.getCmd())) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = getTowedUnit();
            if (other != null) {
                cmd.addStep(MoveStepType.TOW);
                clientgui.getBoardView().drawMovementData(ce(), cmd);
            } // else - didn't find a unit to tow
        } else if (actionCmd.equals(MoveCommand.MOVE_DISCONNECT.getCmd())) {
            // Ask the user if we're carrying multiple units.
            Entity other = getDisconnectedUnit();
            if (other != null) {
                cmd.addStep(MoveStepType.DISCONNECT, other);
                clientgui.getBoardView().drawMovementData(ce(), cmd);
            } // else - didn't find a unit to tow
        } else if (actionCmd.equals(MoveCommand.MOVE_MOUNT.getCmd())) {
            Entity other = getMountedUnit();
            if (other != null) {
                cmd.addStep(MoveStepType.MOUNT, other);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_UNLOAD.getCmd())) {
            // Ask the user if we're carrying multiple units.
            Entity other = getUnloadedUnit();
            if (other != null) {
                if (ce() instanceof SmallCraft 
                        || !ce().getAllTowedUnits().isEmpty()
                        || ce().getTowedBy() != Entity.NONE) {
                    Coords pos = getUnloadPosition(other);
                    if (null != pos) {
                        // set other's position and end this turn - the
                        // unloading unit will get
                        // another turn for further unloading later
                        cmd.addStep(MoveStepType.UNLOAD, other, pos);
                        clientgui.getBoardView().drawMovementData(ce(), cmd);
                        ready();
                    }
                } else {
                    // some different handling for small craft/dropship
                    // unloading
                    cmd.addStep(MoveStepType.UNLOAD, other);
                    clientgui.getBoardView().drawMovementData(ce(), cmd);
                }
            } // else - Player canceled the unload.
        } else if (actionCmd.equals(MoveCommand.MOVE_RAISE_ELEVATION.getCmd())) {
            cmd.addStep(MoveStepType.UP);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_LOWER_ELEVATION.getCmd())) {
            if (ce.isAero()
                    && (null != cmd.getLastStep())
                    && (cmd.getLastStep().getNDown() == 1)
                    && (cmd.getLastStep().getVelocity() < 12)
                    && !(((IAero) ce).isSpheroid() || clientgui.getClient()
                            .getGame().getPlanetaryConditions().isVacuum())) {
                cmd.addStep(MoveStepType.ACC, true);
            }
            cmd.addStep(MoveStepType.DOWN);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_CLIMB_MODE.getCmd())) {
            MoveStep ms = cmd.getLastStep();
            if ((ms != null)
                    && ((ms.getType() == MoveStepType.CLIMB_MODE_ON) || (ms
                            .getType() == MoveStepType.CLIMB_MODE_OFF))) {
                MoveStep lastStep = cmd.getLastStep();
                cmd.removeLastStep();
                // Add another climb mode step
                // Without this, we end up with 3 effect modes: no climb step, climb step on, climb step off
                // This affects how the StepSprite gets rendered, so it's more clear to keep a climb step
                // once one has been added
                if (lastStep.getType() == MoveStepType.CLIMB_MODE_ON) {
                    cmd.addStep(MoveStepType.CLIMB_MODE_OFF);
                } else {
                    cmd.addStep(MoveStepType.CLIMB_MODE_ON);
                }
            } else if (cmd.getFinalClimbMode()) {
                cmd.addStep(MoveStepType.CLIMB_MODE_OFF);
            } else {
                cmd.addStep(MoveStepType.CLIMB_MODE_ON);
            }
            clientgui.getBoardView().drawMovementData(ce(), cmd);
            computeMovementEnvelope(ce);
        } else if (actionCmd.equals(MoveCommand.MOVE_LAY_MINE.getCmd())) {
            int i = chooseMineToLay();
            if (i != -1) {
                Mounted m = ce.getEquipment(i);
                if (m.getMineType() == Mounted.MINE_VIBRABOMB) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                            clientgui.frame);
                    vsd.setVisible(true);
                    m.setVibraSetting(vsd.getSetting());
                }
                if (cmd.getLastStep() == null
                        && ce instanceof BattleArmor
                        && ce.getMovementMode().equals(EntityMovementMode.INF_JUMP)) {
                    cmd.addStep(MoveStepType.START_JUMP);
                    gear = GEAR_JUMP;
                    Color jumpColor = GUIPreferences.getInstance().getColor(
                            GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
                    clientgui.getBoardView().setHighlightColor(jumpColor);
                    computeMovementEnvelope(ce);
                }
                cmd.addStep(MoveStepType.LAY_MINE, i);
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_CALL_SUPPORT.getCmd())) {
            ((Infantry) ce).createLocalSupport();
            clientgui.getClient().sendUpdateEntity(ce());
        } else if (actionCmd.equals(MoveCommand.MOVE_DIG_IN.getCmd())) {
            cmd.addStep(MoveStepType.DIG_IN);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_FORTIFY.getCmd())) {
            cmd.addStep(MoveStepType.FORTIFY);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_TAKE_COVER.getCmd())) {
            cmd.addStep(MoveStepType.TAKE_COVER);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_SHAKE_OFF.getCmd())) {
            cmd.addStep(MoveStepType.SHAKE_OFF_SWARMERS);
            clientgui.getBoardView().drawMovementData(ce(), cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_RECKLESS.getCmd())) {
            cmd.setCareful(false);
        } else if (actionCmd.equals(MoveCommand.MOVE_STRAFE.getCmd())) {
            gear = GEAR_STRAFE;
        } else if (actionCmd.equals(MoveCommand.MOVE_BOMB.getCmd())) {
            if (cmd.getLastStep() == null) {
                cmd.addStep(MoveStepType.NONE);
            }
            cmd.setVTOLBombStep(cmd.getFinalCoords());
            cmd.compile(clientgui.getClient().getGame(), ce, false);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_ACCN.getCmd())) {
            cmd.addStep(MoveStepType.ACCN);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_DECN.getCmd())) {
            cmd.addStep(MoveStepType.DECN);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_ACC.getCmd())) {
            cmd.addStep(MoveStepType.ACC);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_DEC.getCmd())) {
            cmd.addStep(MoveStepType.DEC);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_EVADE.getCmd())) {
            cmd.addStep(MoveStepType.EVADE);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_BOOTLEGGER.getCmd())) {
            cmd.addStep(MoveStepType.BOOTLEGGER);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_SHUTDOWN.getCmd())) {
            if (clientgui.doYesNoDialog(
                    Messages.getString("MovementDisplay.ShutdownDialog.title"),
                    Messages.getString("MovementDisplay.ShutdownDialog.message"))) {
                cmd.addStep(MoveStepType.SHUTDOWN);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_STARTUP.getCmd())) {
            if (clientgui.doYesNoDialog(
                    Messages.getString("MovementDisplay.StartupDialog.title"),
                    Messages.getString("MovementDisplay.StartupDialog.message"))) {
                clear();
                cmd.addStep(MoveStepType.STARTUP);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_SELF_DESTRUCT.getCmd())) {
            if (clientgui.doYesNoDialog(
                    Messages.getString("MovementDisplay.SelfDestructDialog.title"),
                    Messages.getString("MovementDisplay.SelfDestructDialog.message"))) {
                cmd.addStep(MoveStepType.SELF_DESTRUCT);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_EVADE_AERO.getCmd())) {
            cmd.addStep(MoveStepType.EVADE);
            clientgui.getBoardView().drawMovementData(ce, cmd);
            setEvadeAeroEnabled(false);
        } else if (actionCmd.equals(MoveCommand.MOVE_ROLL.getCmd())) {
            cmd.addStep(MoveStepType.ROLL);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_HOVER.getCmd())) {
            cmd.addStep(MoveStepType.HOVER);
            if (ce instanceof LandAirMech
                    && ce.getMovementMode() == EntityMovementMode.WIGE
                    && ce.isAirborne()) {
                gear = GEAR_LAND;
            }
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_MANEUVER.getCmd())) {
            ManeuverChoiceDialog choiceDialog = new ManeuverChoiceDialog(
                    clientgui.frame,
                    Messages.getString("MovementDisplay.ManeuverDialog.title"),
                    "huh?");
            IAero a = (IAero) ce;
            MoveStep last = cmd.getLastStep();
            int vel = a.getCurrentVelocity();
            int altitude = ce.getAltitude();
            Coords pos = ce.getPosition();
            int distance = 0;
            if (null != last) {
                vel = last.getVelocityLeft();
                altitude = last.getAltitude();
                pos = last.getPosition();
                distance = last.getDistance();
            }
            Board board = clientgui.getClient().getGame().getBoard();
            // On Atmospheric maps, elevations are treated as altitudes, so
            // hex ceiling is the ground
            int ceil = board.getHex(pos).ceiling(board.inAtmosphere());
            // On the ground map, Aeros ignore hex elevations
            if (board.onGround()) {
                ceil = 0;
            }
            choiceDialog.checkPerformability(vel, altitude, ceil, a.isVSTOL(),
                    distance, clientgui.getClient().getGame(), cmd);
            choiceDialog.setVisible(true);
            int manType = choiceDialog.getChoice();
            if ((manType > ManeuverType.MAN_NONE) && addManeuver(manType)) {
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_LAUNCH.getCmd())) {
            TreeMap<Integer, Vector<Integer>> undocked = getUndockedUnits();
            if (!undocked.isEmpty()) {
                cmd.addStep(MoveStepType.UNDOCK, undocked);
            }
            TreeMap<Integer, Vector<Integer>> launched = getLaunchedUnits();
            if (!launched.isEmpty()) {
                cmd.addStep(MoveStepType.LAUNCH, launched);
            }
            if (!launched.isEmpty() || !undocked.isEmpty()) {
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_RECOVER.getCmd())
                || actionCmd.equals(MoveCommand.MOVE_DOCK.getCmd())) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int recoverer = getRecoveryUnit();
            if (recoverer != -1) {
                cmd.addStep(MoveStepType.RECOVER, recoverer, -1);
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
            if (actionCmd.equals(MoveCommand.MOVE_DOCK.getCmd())) {
                cmd.getLastStep().setDocking(true);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_DROP.getCmd())) {
            TreeMap<Integer, Vector<Integer>> dropped = getDroppedUnits();
            if (!dropped.isEmpty()) {
                cmd.addStep(MoveStepType.DROP, dropped);
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_JOIN.getCmd())) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int joined = getUnitJoined();
            if (joined != -1) {
                cmd.addStep(MoveStepType.JOIN, joined, -1);
                clientgui.getBoardView().drawMovementData(ce, cmd);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN_LEFT.getCmd())) {
            cmd.addStep(MoveStepType.TURN_LEFT);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN_RIGHT.getCmd())) {
            cmd.addStep(MoveStepType.TURN_RIGHT);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_THRUST.getCmd())) {
            cmd.addStep(MoveStepType.THRUST);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_YAW.getCmd())) {
            cmd.addStep(MoveStepType.YAW);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_END_OVER.getCmd())) {
            cmd.addStep(MoveStepType.YAW);
            cmd.addStep(MoveStepType.ROLL);
            clientgui.getBoardView().drawMovementData(ce, cmd);
        } else if (actionCmd.equals(MoveCommand.MOVE_DUMP.getCmd())) {
            dumpBombs();
        } else if (actionCmd.equals(MoveCommand.MOVE_TAKE_OFF.getCmd())) {
            if (ce().isAero()
                    && (null != ((IAero) ce()).hasRoomForHorizontalTakeOff())) {
                String title = Messages.getString("MovementDisplay.NoTakeOffDialog.title");
                String body = Messages.getString(
                        "MovementDisplay.NoTakeOffDialog.message",
                        new Object[] { ((IAero) ce())
                                .hasRoomForHorizontalTakeOff() });
                clientgui.doAlertDialog(title, body);
            } else {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.TakeOffDialog.title"),
                        Messages.getString("MovementDisplay.TakeOffDialog.message"))) {
                    clear();
                    cmd.addStep(MoveStepType.TAKEOFF);
                    ready();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_VERT_TAKE_OFF.getCmd())) {
            if (clientgui.doYesNoDialog(
                    Messages.getString("MovementDisplay.TakeOffDialog.title"),
                    Messages.getString("MovementDisplay.TakeOffDialog.message"))) {
                clear();
                cmd.addStep(MoveStepType.VTAKEOFF);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_LAND.getCmd())) {
            if (ce().isAero()
                    && (null != ((IAero) ce()).hasRoomForHorizontalLanding())) {
                String title = Messages.getString("MovementDisplay.NoLandingDialog.title");
                String body = Messages.getString("MovementDisplay.NoLandingDialog.message",
                        ((IAero) ce()).hasRoomForHorizontalLanding());
                clientgui.doAlertDialog(title, body);
            } else {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.LandDialog.title"),
                        Messages.getString("MovementDisplay.LandDialog.message"))) {
                    clear();
                    cmd.addStep(MoveStepType.LAND);
                    ready();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_VERT_LAND.getCmd())) {
            if (ce().isAero()
                    && (null != ((IAero) ce()).hasRoomForVerticalLanding())) {
                String title = Messages.getString("MovementDisplay.NoLandingDialog.title");
                String body = Messages.getString("MovementDisplay.NoLandingDialog.message",
                        ((IAero) ce()).hasRoomForVerticalLanding());
                clientgui.doAlertDialog(title, body);
            } else {
                if (clientgui.doYesNoDialog(
                        Messages.getString("MovementDisplay.LandDialog.title"),
                        Messages.getString("MovementDisplay.LandDialog.message"))) {
                    clear();
                    cmd.addStep(MoveStepType.VLAND);
                    ready();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_ENVELOPE.getCmd())) {
            computeMovementEnvelope(clientgui.getUnitDisplay().getCurrentEntity());
        } else if (actionCmd.equals(MoveCommand.MOVE_TRAITOR.getCmd())) {
            var players = clientgui.getClient().getGame().getPlayersVector();
            Integer[] playerIds = new Integer[players.size() - 1];
            String[] playerNames = new String[players.size() - 1];
            String[] options = new String[players.size() - 1];
            Entity e = ce();

            // Loop through the players vector and fill in the arrays
            int idx = 0;
            for (var player : players) {
                if (player.getName().equals(clientgui.getClient().getLocalPlayer().getName())
                        || (player.getTeam() == Player.TEAM_UNASSIGNED)) {
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
                        "No players available. Units cannot be traitored to players "
                        + "that aren't assigned to a team.");
                return;
            }

            // Dialog for choosing which player to transfer to
            String option = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                            "Choose the player to gain ownership of this unit when it turns traitor",
                            "Traitor", JOptionPane.QUESTION_MESSAGE, null,
                            options, options[0]);

            // Verify that we have a valid option...
            if (option != null) {
                // Now that we've selected a player, correctly associate the ID and name
                int id = playerIds[Arrays.asList(options).indexOf(option)];
                String name = playerNames[Arrays.asList(options).indexOf(option)];

                // And now we perform the actual transfer
                int confirm = JOptionPane.showConfirmDialog(
                                clientgui.getFrame(),
                                e.getDisplayName() + " will switch to " + name
                                        + "'s side at the end of this turn. Are you sure?", 
                                "Confirm",
                                JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    e.setTraitorId(id);
                    clientgui.getClient().sendUpdateEntity(e);
                }
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

        // if small craft/dropship that has unloaded units, then only allowed
        // to unload more
        if (ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }

    }
    
    /**
     * Add enough <code>MoveStepType.CONVERT_MODE</code> steps to get to the requested mode, or
     * clear the path if the unit is in the requested mode at the beginning of the turn.
     * 
     * @param endMode The mode to convert to
     */
    private void adjustConvertSteps(EntityMovementMode endMode) {
        //Since conversion is not allowed in water, we shouldn't have to deal with the possibility of swim modes.
        if (ce().getMovementMode() == endMode
                // Account for grounded LAMs in fighter mode with movement type wheeled 
                || (ce().isAero() && endMode == EntityMovementMode.AERODYNE)) {
            cmd.clear();
            return;
        }
        if (cmd.getFinalConversionMode() == endMode) {
            return;
        }
        clear();
        ce().setConvertingNow(true);
        cmd.addStep(MoveStepType.CONVERT_MODE);
        if (cmd.getFinalConversionMode() != endMode) {
            cmd.addStep(MoveStepType.CONVERT_MODE);
        }
        if (ce() instanceof Mech && ((Mech) ce()).hasTracks()) {
            ce().setMovementMode(endMode);
        }
    }

    /**
     * Give the player the opportunity to unload all entities that are stranded
     * on immobile transports.
     * <p>
     * According to
     * <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">Randall Bills</a>,
     * the "minimum move" rule allow stranded units to dismount at the start of the turn.
     */
    private void unloadStranded() {
        Vector<Entity> stranded = new Vector<>();
        String[] names = null;
        Entity entity = null;
        Entity transport = null;

        // Let the player know what's going on.
        setStatusBarText(Messages.getString("MovementDisplay.AllPlayersUnload"));

        // Collect the stranded entities into the vector.
        Iterator<Entity> entities = clientgui.getClient().getSelectedEntities(
                new EntitySelector() {
                    private final Game game = clientgui.getClient().getGame();
                    private final GameTurn turn = clientgui.getClient().getGame().getTurn();
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
                buffer = Messages.getString("MovementDisplay.EntityAt", entity.getDisplayName(),
                        transport.getPosition().getBoardNum());
            }
            names[index] = buffer;
        }

        // Show the choices to the player
        int[] indexes = clientgui.doChoiceDialog(
                Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.title"),
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
        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (clientgui.getClient().isMyTurn() && (ce != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.getBoardView().centerOnHex(ce.getPosition());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        Entity e = clientgui.getClient().getGame().getEntity(b.getEntityId());
        if (null == e) {
            return;
        }
        if (clientgui.getClient().isMyTurn()) {
            if (clientgui.getClient().getGame().getTurn().isValidEntity(e, clientgui.getClient().getGame())) {
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
        // forward initiative can only be done if Teams have an initiative!
        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_TEAM_INITIATIVE)) {
            getBtn(MoveCommand.MOVE_FORWARD_INI).setEnabled(enabled);
            clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FORWARD_INI.getCmd(), enabled);
        } else { // turn them off regardless what is said!
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
        getBtn(MoveCommand.MOVE_FLY_OFF).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FLY_OFF.getCmd(), enabled);
    }

    private void setEjectEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_EJECT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_EJECT.getCmd(), enabled);
    }

    private void setUnjamEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_UNJAM).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_UNJAM.getCmd(), enabled);
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
        getBtn(MoveCommand.MOVE_ACCN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_ACCN.getCmd(), enabled);
    }

    private void setDecNEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DECN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DECN.getCmd(), enabled);
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

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        if (clientgui != null) {
            clientgui.getClient().getGame().removeGameListener(this);
            clientgui.getBoardView().removeBoardViewListener(this);
        }
    }

    private void updateTurnButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        setTurnEnabled(!ce.isImmobile() && !ce.isStuck() && ((ce.getWalkMP() > 0) || (ce.getJumpMP() > 0))
                && !(cmd.isJumping() && (ce instanceof Mech) && (ce.getJumpType() == Mech.JUMP_BOOSTER)));
    }
    
    public void FieldOfFire(Entity unit, int[][] ranges, int arc, int loc) {
        // do nothing here outside the movement phase
        if (!clientgui.getClient().getGame().getPhase().isMovement()) {
            return;
        }

        clientgui.getBoardView().fieldofFireUnit = unit;
        clientgui.getBoardView().fieldofFireRanges = ranges;
        clientgui.getBoardView().fieldofFireWpArc = arc;
        clientgui.getBoardView().fieldofFireWpLoc = loc;

        // If the unit is the current unit, then work with
        // the current considered movement
        if (unit.equals(ce())) {
            clientgui.getBoardView().setWeaponFieldofFire(ce(), cmd);
        } else {
            clientgui.getBoardView().setWeaponFieldofFire(unit.getFacing(), unit.getPosition());
        }
    }
}
