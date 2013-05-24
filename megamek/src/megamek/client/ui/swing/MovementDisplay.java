/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Bay;
import megamek.common.BipedMech;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.EntitySelector;
import megamek.common.FighterSquadron;
import megamek.common.GameTurn;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.ManeuverType;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Protomech;
import megamek.common.Report;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.TeleMissile;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class MovementDisplay extends StatusBarPhaseDisplay implements
        KeyListener {
    /**
     *
     */
    private static final long serialVersionUID = -7246715124042905688L;

    private static final int NUM_BUTTON_LAYOUTS = 4;

    public static final String MOVE_WALK = "moveWalk"; //$NON-NLS-1$
    public static final String MOVE_NEXT = "moveNext"; //$NON-NLS-1$
    public static final String MOVE_FORWARD_INI = "moveForwardIni"; //$NON-NLS-1$
    public static final String MOVE_JUMP = "moveJump"; //$NON-NLS-1$
    public static final String MOVE_BACK_UP = "moveBackUp"; //$NON-NLS-1$
    public static final String MOVE_TURN = "moveTurn"; //$NON-NLS-1$
    public static final String MOVE_GET_UP = "moveGetUp"; //$NON-NLS-1$
    public static final String MOVE_CHARGE = "moveCharge"; //$NON-NLS-1$
    public static final String MOVE_DFA = "moveDFA"; //$NON-NLS-1$
    public static final String MOVE_GO_PRONE = "moveGoProne"; //$NON-NLS-1$
    public static final String MOVE_FLEE = "moveFlee"; //$NON-NLS-1$
    public static final String MOVE_EJECT = "moveEject"; //$NON-NLS-1$
    public static final String MOVE_LOAD = "moveLoad"; //$NON-NLS-1$
    public static final String MOVE_UNLOAD = "moveUnload"; //$NON-NLS-1$
    public static final String MOVE_MOUNT = "moveMount"; //$NON-NLS-1$
    public static final String MOVE_UNJAM = "moveUnjam"; //$NON-NLS-1$
    public static final String MOVE_CLEAR = "moveClear"; //$NON-NLS-1$
    public static final String MOVE_CANCEL = "moveCancel"; //$NON-NLS-1$
    public static final String MOVE_RAISE_ELEVATION = "moveRaiseElevation"; //$NON-NLS-1$
    public static final String MOVE_LOWER_ELEVATION = "moveLowerElevation"; //$NON-NLS-1$
    public static final String MOVE_SEARCHLIGHT = "moveSearchlight"; //$NON-NLS-1$
    public static final String MOVE_LAY_MINE = "moveLayMine"; //$NON-NLS-1$
    public static final String MOVE_HULL_DOWN = "moveHullDown"; //$NON-NLS-1$
    public static final String MOVE_CLIMB_MODE = "moveClimbMode"; //$NON-NLS-1$
    public static final String MOVE_SWIM = "moveSwim"; //$NON-NLS-1$
    public static final String MOVE_DIG_IN = "moveDigIn"; //$NON-NLS-1$
    public static final String MOVE_FORTIFY = "moveFortify"; //$NON-NLS-1$
    public static final String MOVE_SHAKE_OFF = "moveShakeOff"; //$NON-NLS-1$
    public static final String MOVE_MODE_MECH = "moveModeMech"; //$NON-NLS-1$
    public static final String MOVE_MODE_AIRMECH = "moveModeAirmech"; //$NON-NLS-1$
    public static final String MOVE_MODE_AIRCRAFT = "moveModeAircraft"; //$NON-NLS-1$
    public static final String MOVE_RECKLESS = "moveReckless"; //$NON-NLS-1$
    public static final String MOVE_CAREFUL_STAND = "moveCarefulStand"; //$NON-NLS-1$
    public static final String MOVE_EVADE = "MoveEvade"; //$NON-NLS-1$
    public static final String MOVE_SHUTDOWN = "moveShutDown"; //$NON-NLS-1$
    public static final String MOVE_STARTUP = "moveStartup"; //$NON-NLS-1$
    public static final String MOVE_SELF_DESTRUCT = "moveSelfDestruct"; //$NON-NLS-1$
    // Aero Movement
    public static final String MOVE_ACC = "MoveAccelerate"; //$NON-NLS-1$
    public static final String MOVE_DEC = "MoveDecelerate"; //$NON-NLS-1$
    public static final String MOVE_EVADE_AERO = "MoveEvadeAero"; //$NON-NLS-1$
    public static final String MOVE_ACCN = "MoveAccNext"; //$NON-NLS-1$
    public static final String MOVE_DECN = "MoveDecNext"; //$NON-NLS-1$
    public static final String MOVE_ROLL = "MoveRoll"; //$NON-NLS-1$
    public static final String MOVE_LAUNCH = "MoveLaunch"; //$NON-NLS-1$
    public static final String MOVE_RECOVER = "MoveRecover"; //$NON-NLS-1$
    public static final String MOVE_DROP = "MoveDrop"; //$NON-NLS-1$
    public static final String MOVE_DUMP = "MoveDump"; //$NON-NLS-1$
    public static final String MOVE_RAM = "MoveRam"; //$NON-NLS-1$
    public static final String MOVE_HOVER = "MoveHover"; //$NON-NLS-1$
    public static final String MOVE_MANEUVER = "MoveManeuver"; //$NON-NLS-1$
    public static final String MOVE_JOIN = "MoveJoin"; //$NON-NLS-1$
    public static final String MOVE_FLY_OFF = "MoveOff"; //$NON-NLS-1$
    public static final String MOVE_TAKE_OFF = "MoveTakeOff"; //$NON-NLS-1$
    public static final String MOVE_VERT_TAKE_OFF = "MoveVertTakeOff"; //$NON-NLS-1$
    public static final String MOVE_LAND = "MoveLand"; //$NON-NLS-1$
    public static final String MOVE_VERT_LAND = "MoveVLand"; //$NON-NLS-1$
    // Aero Vector Movement
    public static final String MOVE_TURN_LEFT = "MoveTurnLeft"; //$NON-NLS-1$
    public static final String MOVE_TURN_RIGHT = "MoveTurnRight"; //$NON-NLS-1$
    public static final String MOVE_THRUST = "MoveThrust"; //$NON-NLS-1$
    public static final String MOVE_YAW = "MoveYaw"; //$NON-NLS-1$
    public static final String MOVE_END_OVER = "MoveEndOver"; //$NON-NLS-1$

    // buttons
    private JPanel panButtons;
    private JButton butWalk;
    private JButton butJump;
    private JButton butBackup;
    private JButton butTurn;
    private JButton butSwim;
    private JButton butUp;
    private JButton butDown;
    private JButton butCharge;
    private JButton butDfa;
    private JButton butRAC;
    private JButton butFlee;
    private JButton butFlyOff;
    private JButton butEject;
    private JButton butLoad;
    private JButton butUnload;
    private JButton butMount;
    private JButton butClear;
    private JButton butNext;
    private JButton butForwardIni;
    private JButton butMore;
    private JButton butRaise;
    private JButton butLower;
    private JButton butSearchlight;
    private JButton butLayMine;
    private JButton butHullDown;
    private JButton butClimbMode;
    private JButton butDigIn;
    private JButton butFortify;
    private JButton butShakeOff;
    private JButton butReckless;
    private JButton butEvade;
    private JButton butShutdown;
    private JButton butStartup;
    private JButton butSelfDestruct;

    private JButton butAcc;
    private JButton butDec;
    private JButton butEvadeAero;
    private JButton butAccN;
    private JButton butDecN;
    private JButton butRoll;
    private JButton butLaunch;
    private JButton butRecover;
    private JButton butDrop;
    private JButton butDump;
    private JButton butRam;
    private JButton butHover;
    private JButton butManeuver;
    private JButton butJoin;
    private JButton butTakeOff;
    private JButton butVTakeOff;
    private JButton butLand;
    private JButton butVLand;

    private JButton butTurnLeft;
    private JButton butTurnRight;
    private JButton butThrust;
    private JButton butYaw;
    private JButton butEndOver;

    private int buttonLayout;
    // order of buttons for various entity types
    private ArrayList<JButton> buttonsMech;
    private ArrayList<JButton> buttonsTank;
    private ArrayList<JButton> buttonsVtol;
    private ArrayList<JButton> buttonsInf;
    private ArrayList<JButton> buttonsAero;
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

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public MovementDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        clientgui.getClient().game.addGameListener(this);
        gear = MovementDisplay.GEAR_LAND;
        shiftheld = false;
        clientgui.getBoardView().addBoardViewListener(this);
        clientgui.getClient().game.setupTeams();
        setupStatusBar(Messages
                .getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$
        butClear = new JButton(Messages.getString("MovementDisplay.butClear")); //$NON-NLS-1$
        butClear.addActionListener(this);
        butClear.setEnabled(false);
        butClear.setActionCommand(MOVE_CLEAR);
        butClear.addKeyListener(this);
        butWalk = new JButton(Messages.getString("MovementDisplay.butWalk")); //$NON-NLS-1$
        butWalk.addActionListener(this);
        butWalk.setEnabled(false);
        butWalk.setActionCommand(MOVE_WALK);
        butWalk.addKeyListener(this);
        butJump = new JButton(Messages.getString("MovementDisplay.butJump")); //$NON-NLS-1$
        butJump.addActionListener(this);
        butJump.setEnabled(false);
        butJump.setActionCommand(MOVE_JUMP);
        butJump.addKeyListener(this);
        butSwim = new JButton(Messages.getString("MovementDisplay.butSwim")); //$NON-NLS-1$
        butSwim.addActionListener(this);
        butSwim.setEnabled(false);
        butSwim.setActionCommand(MOVE_SWIM);
        butSwim.addKeyListener(this);
        butBackup = new JButton(Messages.getString("MovementDisplay.butBackup")); //$NON-NLS-1$
        butBackup.addActionListener(this);
        butBackup.setEnabled(false);
        butBackup.setActionCommand(MOVE_BACK_UP);
        butBackup.addKeyListener(this);
        butTurn = new JButton(Messages.getString("MovementDisplay.butTurn")); //$NON-NLS-1$
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
        butTurn.setActionCommand(MOVE_TURN);
        butTurn.addKeyListener(this);
        butUp = new JButton(Messages.getString("MovementDisplay.butUp")); //$NON-NLS-1$
        butUp.addActionListener(this);
        butUp.setEnabled(false);
        butUp.setActionCommand(MOVE_GET_UP);
        butUp.addKeyListener(this);
        butDown = new JButton(Messages.getString("MovementDisplay.butDown")); //$NON-NLS-1$
        butDown.addActionListener(this);
        butDown.setEnabled(false);
        butDown.setActionCommand(MOVE_GO_PRONE);
        butDown.addKeyListener(this);
        butCharge = new JButton(Messages.getString("MovementDisplay.butCharge")); //$NON-NLS-1$
        butCharge.addActionListener(this);
        butCharge.setEnabled(false);
        butCharge.setActionCommand(MOVE_CHARGE);
        butCharge.addKeyListener(this);
        butDfa = new JButton(Messages.getString("MovementDisplay.butDfa")); //$NON-NLS-1$
        butDfa.addActionListener(this);
        butDfa.setEnabled(false);
        butDfa.setActionCommand(MOVE_DFA);
        butDfa.addKeyListener(this);
        butFlee = new JButton(Messages.getString("MovementDisplay.butFlee")); //$NON-NLS-1$
        butFlee.addActionListener(this);
        butFlee.setEnabled(false);
        butFlee.setActionCommand(MOVE_FLEE);
        butFlee.addKeyListener(this);
        butFlyOff = new JButton(Messages.getString("MovementDisplay.butFlyOff")); //$NON-NLS-1$
        butFlyOff.addActionListener(this);
        butFlyOff.setEnabled(false);
        butFlyOff.setActionCommand(MOVE_FLY_OFF);
        butFlyOff.addKeyListener(this);
        butEject = new JButton(Messages.getString("MovementDisplay.butEject")); //$NON-NLS-1$
        butEject.addActionListener(this);
        butEject.setEnabled(false);
        butEject.setActionCommand(MOVE_EJECT);
        butEject.addKeyListener(this);
        butRAC = new JButton(Messages.getString("MovementDisplay.butRAC")); //$NON-NLS-1$
        butRAC.addActionListener(this);
        butRAC.setEnabled(false);
        butRAC.setActionCommand(MOVE_UNJAM);
        butRAC.addKeyListener(this);
        butSearchlight = new JButton(Messages
                .getString("MovementDisplay.butSearchlightOn")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.setEnabled(false);
        butSearchlight.setActionCommand(MOVE_SEARCHLIGHT);
        butSearchlight.addKeyListener(this);
        butMore = new JButton(Messages.getString("MovementDisplay.butMore")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.setEnabled(false);
        butMore.addKeyListener(this);
        butNext = new JButton(Messages.getString("MovementDisplay.butNext")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(MOVE_NEXT);
        butNext.addKeyListener(this);
        butForwardIni = new JButton(Messages.getString("MovementDisplay.butForwardIni")); //$NON-NLS-1$
        butForwardIni.addActionListener(this);
        butForwardIni.setEnabled(false);
        butForwardIni.setActionCommand(MOVE_FORWARD_INI);
        butForwardIni.addKeyListener(this);
        butDone.setText("<html><b>"+Messages.getString("MovementDisplay.butDone")+"</b></html>"); //$NON-NLS-1$
        butDone.setEnabled(false);
        butLoad = new JButton(Messages.getString("MovementDisplay.butLoad")); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);
        butLoad.setActionCommand(MOVE_LOAD);
        butLoad.addKeyListener(this);
        butMount = new JButton(Messages.getString("MovementDisplay.butMount")); //$NON-NLS-1$
        butMount.addActionListener(this);
        butMount.setEnabled(false);
        butMount.setActionCommand(MOVE_MOUNT);
        butMount.addKeyListener(this);
        butUnload = new JButton(Messages.getString("MovementDisplay.butUnload")); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);
        butUnload.setActionCommand(MOVE_UNLOAD);
        butUnload.addKeyListener(this);
        butRaise = new JButton(Messages.getString("MovementDisplay.butRaise")); //$NON-NLS-1$
        butRaise.addActionListener(this);
        butRaise.setEnabled(false);
        butRaise.setActionCommand(MOVE_RAISE_ELEVATION);
        butRaise.addKeyListener(this);
        butLower = new JButton(Messages.getString("MovementDisplay.butLower")); //$NON-NLS-1$
        butLower.addActionListener(this);
        butLower.setEnabled(false);
        butLower.setActionCommand(MOVE_LOWER_ELEVATION);
        butLower.addKeyListener(this);
        butLayMine = new JButton(Messages
                .getString("MovementDisplay.butLayMine")); //$NON-NLS-1$
        butLayMine.addActionListener(this);
        butLayMine.setEnabled(false);
        butLayMine.setActionCommand(MOVE_LAY_MINE);
        butLayMine.addKeyListener(this);
        butHullDown = new JButton(Messages
                .getString("MovementDisplay.butHullDown")); //$NON-NLS-1$
        butHullDown.addActionListener(this);
        butHullDown.setEnabled(false);
        butHullDown.setActionCommand(MOVE_HULL_DOWN);
        butHullDown.addKeyListener(this);
        butClimbMode = new JButton(Messages
                .getString("MovementDisplay.butClimbMode")); //$NON-NLS-1$
        butClimbMode.addActionListener(this);
        butClimbMode.setEnabled(false);
        butClimbMode.setActionCommand(MOVE_CLIMB_MODE);
        butClimbMode.addKeyListener(this);
        butDigIn = new JButton(Messages.getString("MovementDisplay.butDigIn")); //$NON-NLS-1$
        butDigIn.addActionListener(this);
        butDigIn.setEnabled(false);
        butDigIn.setActionCommand(MOVE_DIG_IN);
        butDigIn.addKeyListener(this);
        butFortify = new JButton(Messages
                .getString("MovementDisplay.butFortify")); //$NON-NLS-1$
        butFortify.addActionListener(this);
        butFortify.setEnabled(false);
        butFortify.setActionCommand(MOVE_FORTIFY);
        butFortify.addKeyListener(this);
        butShakeOff = new JButton(Messages
                .getString("MovementDisplay.butShakeOff")); //$NON-NLS-1$
        butShakeOff.addActionListener(this);
        butShakeOff.setEnabled(false);
        butShakeOff.setActionCommand(MOVE_SHAKE_OFF);
        butShakeOff.addKeyListener(this);

        butReckless = new JButton(Messages
                .getString("MovementDisplay.butReckless")); //$NON-NLS-1$
        butReckless.addActionListener(this);
        butReckless.setEnabled(false);
        butReckless.setActionCommand(MOVE_RECKLESS);
        butReckless.addKeyListener(this);

        butEvade = new JButton(Messages.getString("MovementDisplay.butEvade")); //$NON-NLS-1$
        butEvade.addActionListener(this);
        butEvade.setEnabled(false);
        butEvade.setActionCommand(MOVE_EVADE);
        butEvade.addKeyListener(this);

        butShutdown = new JButton(Messages.getString("MovementDisplay.butShutdown")); //$NON-NLS-1$
        butShutdown.addActionListener(this);
        butShutdown.setEnabled(false);
        butShutdown.setActionCommand(MOVE_SHUTDOWN);
        butShutdown.addKeyListener(this);

        butStartup = new JButton(Messages.getString("MovementDisplay.butStartup")); //$NON-NLS-1$
        butStartup.addActionListener(this);
        butStartup.setEnabled(false);
        butStartup.setActionCommand(MOVE_STARTUP);
        butStartup.addKeyListener(this);

        butSelfDestruct = new JButton(Messages.getString("MovementDisplay.butSelfDestruct")); //$NON-NLS-1$
        butSelfDestruct.addActionListener(this);
        butSelfDestruct.setEnabled(false);
        butSelfDestruct.setActionCommand(MOVE_SELF_DESTRUCT);
        butSelfDestruct.addKeyListener(this);

        butAcc = new JButton(Messages.getString("MovementDisplay.butAcc")); //$NON-NLS-1$
        butAcc.addActionListener(this);
        butAcc.setEnabled(false);
        butAcc.setActionCommand(MOVE_ACC);
        butAcc.addKeyListener(this);

        butDec = new JButton(Messages.getString("MovementDisplay.butDec")); //$NON-NLS-1$
        butDec.addActionListener(this);
        butDec.setEnabled(false);
        butDec.setActionCommand(MOVE_DEC);
        butDec.addKeyListener(this);

        butAccN = new JButton(Messages.getString("MovementDisplay.butAccN")); //$NON-NLS-1$
        butAccN.addActionListener(this);
        butAccN.setEnabled(false);
        butAccN.setActionCommand(MOVE_ACCN);
        butAccN.addKeyListener(this);

        butDecN = new JButton(Messages.getString("MovementDisplay.butDecN")); //$NON-NLS-1$
        butDecN.addActionListener(this);
        butDecN.setEnabled(false);
        butDecN.setActionCommand(MOVE_DECN);
        butDecN.addKeyListener(this);

        butEvadeAero = new JButton(Messages
                .getString("MovementDisplay.butEvadeAero")); //$NON-NLS-1$
        butEvadeAero.addActionListener(this);
        butEvadeAero.setEnabled(false);
        butEvadeAero.setActionCommand(MOVE_EVADE_AERO);
        butEvadeAero.addKeyListener(this);

        butRoll = new JButton(Messages.getString("MovementDisplay.butRoll")); //$NON-NLS-1$
        butRoll.addActionListener(this);
        butRoll.setEnabled(false);
        butRoll.setActionCommand(MOVE_ROLL);
        butRoll.addKeyListener(this);

        butLaunch = new JButton(Messages.getString("MovementDisplay.butLaunch")); //$NON-NLS-1$
        butLaunch.addActionListener(this);
        butLaunch.setEnabled(false);
        butLaunch.setActionCommand(MOVE_LAUNCH);
        butLaunch.addKeyListener(this);

        butRecover = new JButton(Messages
                .getString("MovementDisplay.butRecover")); //$NON-NLS-1$
        butRecover.addActionListener(this);
        butRecover.setEnabled(false);
        butRecover.setActionCommand(MOVE_RECOVER);
        butRecover.addKeyListener(this);

        butDrop = new JButton(Messages.getString("MovementDisplay.butDrop")); //$NON-NLS-1$
        butDrop.addActionListener(this);
        butDrop.setEnabled(false);
        butDrop.setActionCommand(MOVE_DROP);
        butDrop.addKeyListener(this);

        butJoin = new JButton(Messages.getString("MovementDisplay.butJoin")); //$NON-NLS-1$
        butJoin.addActionListener(this);
        butJoin.setEnabled(false);
        butJoin.setActionCommand(MOVE_JOIN);
        butJoin.addKeyListener(this);

        butDump = new JButton(Messages.getString("MovementDisplay.butDump")); //$NON-NLS-1$
        butDump.addActionListener(this);
        butDump.setEnabled(false);
        butDump.setActionCommand(MOVE_DUMP);
        butDump.addKeyListener(this);

        butRam = new JButton(Messages.getString("MovementDisplay.butRam")); //$NON-NLS-1$
        butRam.addActionListener(this);
        butRam.setEnabled(false);
        butRam.setActionCommand(MOVE_RAM);
        butRam.addKeyListener(this);

        butHover = new JButton(Messages.getString("MovementDisplay.butHover")); //$NON-NLS-1$
        butHover.addActionListener(this);
        butHover.setEnabled(false);
        butHover.setActionCommand(MOVE_HOVER);
        butHover.addKeyListener(this);

        butTakeOff = new JButton(Messages.getString("MovementDisplay.butTakeOff")); //$NON-NLS-1$
        butTakeOff.addActionListener(this);
        butTakeOff.setEnabled(false);
        butTakeOff.setActionCommand(MOVE_TAKE_OFF);
        butTakeOff.addKeyListener(this);

        butVTakeOff = new JButton(Messages.getString("MovementDisplay.butVTakeOff")); //$NON-NLS-1$
        butVTakeOff.addActionListener(this);
        butVTakeOff.setEnabled(false);
        butVTakeOff.setActionCommand(MOVE_VERT_TAKE_OFF);
        butVTakeOff.addKeyListener(this);

        butLand = new JButton(Messages.getString("MovementDisplay.butLand")); //$NON-NLS-1$
        butLand.addActionListener(this);
        butLand.setEnabled(false);
        butLand.setActionCommand(MOVE_LAND);
        butLand.addKeyListener(this);

        butVLand = new JButton(Messages.getString("MovementDisplay.butVLand")); //$NON-NLS-1$
        butVLand.addActionListener(this);
        butVLand.setEnabled(false);
        butVLand.setActionCommand(MOVE_VERT_LAND);
        butVLand.addKeyListener(this);

        butManeuver = new JButton(Messages
                .getString("MovementDisplay.butManeuver")); //$NON-NLS-1$
        butManeuver.addActionListener(this);
        butManeuver.setEnabled(false);
        butManeuver.setActionCommand(MOVE_MANEUVER);
        butManeuver.addKeyListener(this);

        butTurnLeft = new JButton(Messages
                .getString("MovementDisplay.butTurnLeft")); //$NON-NLS-1$
        butTurnLeft.addActionListener(this);
        butTurnLeft.setEnabled(false);
        butTurnLeft.setActionCommand(MOVE_TURN_LEFT);
        butTurnLeft.addKeyListener(this);

        butTurnRight = new JButton(Messages
                .getString("MovementDisplay.butTurnRight")); //$NON-NLS-1$
        butTurnRight.addActionListener(this);
        butTurnRight.setEnabled(false);
        butTurnRight.setActionCommand(MOVE_TURN_RIGHT);
        butTurnRight.addKeyListener(this);

        butThrust = new JButton(Messages.getString("MovementDisplay.butThrust")); //$NON-NLS-1$
        butThrust.addActionListener(this);
        butThrust.setEnabled(false);
        butThrust.setActionCommand(MOVE_THRUST);
        butThrust.addKeyListener(this);

        butYaw = new JButton(Messages.getString("MovementDisplay.butYaw")); //$NON-NLS-1$
        butYaw.addActionListener(this);
        butYaw.setEnabled(false);
        butYaw.setActionCommand(MOVE_YAW);
        butYaw.addKeyListener(this);

        butEndOver = new JButton(Messages
                .getString("MovementDisplay.butEndOver")); //$NON-NLS-1$
        butEndOver.addActionListener(this);
        butEndOver.setEnabled(false);
        butEndOver.setActionCommand(MOVE_END_OVER);
        butEndOver.addKeyListener(this);

        // add buttons to the lists, except space, more & next
        buttonsMech = new ArrayList<JButton>(21);
        buttonsMech.add(butWalk);
        buttonsMech.add(butJump);
        buttonsMech.add(butBackup);
        buttonsMech.add(butTurn);
        buttonsMech.add(butUp);
        buttonsMech.add(butDown);
        buttonsMech.add(butCharge);
        buttonsMech.add(butDfa);
        buttonsMech.add(butLoad);
        buttonsMech.add(butMount);
        buttonsMech.add(butUnload);
        buttonsMech.add(butClimbMode);
        buttonsMech.add(butSearchlight);
        buttonsMech.add(butHullDown);
        buttonsMech.add(butEvade);
        if (clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
            buttonsMech.add(butShutdown);
            buttonsMech.add(butStartup);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("tacops_self_destruct")) {
            buttonsMech.add(butSelfDestruct);
        }
        buttonsMech.add(butReckless);
        buttonsMech.add(butSwim);
        buttonsMech.add(butRaise);
        buttonsMech.add(butLower);
        buttonsMech.add(butEject);
        buttonsMech.add(butFlee);
        buttonsMech.add(butRAC);
        if ((clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()) != null) &&
                (clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()).getSize() > 1)) {
        buttonsMech.add(butForwardIni);
        }

        buttonsTank = new ArrayList<JButton>(23);
        buttonsTank.add(butWalk);
        buttonsTank.add(butBackup);
        buttonsTank.add(butTurn);
        buttonsTank.add(butLoad);
        buttonsTank.add(butMount);
        buttonsTank.add(butUnload);
        buttonsTank.add(butCharge);
        buttonsTank.add(butClimbMode);
        buttonsTank.add(butSearchlight);
        buttonsTank.add(butHullDown);
        buttonsTank.add(butEvade);
        buttonsTank.add(butReckless);
        buttonsTank.add(butEject);
        buttonsTank.add(butFlee);
        buttonsTank.add(butRAC);
        buttonsTank.add(butJump);
        buttonsTank.add(butLayMine);
        buttonsTank.add(butShakeOff);
        buttonsTank.add(butRaise);
        buttonsTank.add(butLower);
        buttonsTank.add(butTakeOff);
        buttonsTank.add(butVTakeOff);
        if ((clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()) != null) &&
                (clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()).getSize() > 1)) {
        buttonsTank.add(butForwardIni);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
            buttonsTank.add(butShutdown);
            buttonsTank.add(butStartup);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("tacops_self_destruct")) {
            buttonsTank.add(butSelfDestruct);
        }

        buttonsVtol = new ArrayList<JButton>(16);
        buttonsVtol.add(butWalk);
        buttonsVtol.add(butBackup);
        buttonsVtol.add(butLower);
        buttonsVtol.add(butRaise);
        buttonsVtol.add(butTurn);
        buttonsVtol.add(butLoad);
        buttonsVtol.add(butMount);
        buttonsVtol.add(butUnload);
        buttonsVtol.add(butSearchlight);
        buttonsVtol.add(butEvade);
        buttonsVtol.add(butReckless);
        buttonsVtol.add(butEject);
        buttonsVtol.add(butFlee);
        buttonsVtol.add(butRAC);
        buttonsVtol.add(butShakeOff);
        if ((clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()) != null) &&
                (clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()).getSize() > 1)) {
        buttonsVtol.add(butForwardIni);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
            buttonsVtol.add(butShutdown);
            buttonsVtol.add(butStartup);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("tacops_self_destruct")) {
            buttonsVtol.add(butSelfDestruct);
        }

        buttonsInf = new ArrayList<JButton>(17);
        buttonsInf.add(butWalk);
        buttonsInf.add(butJump);
        buttonsInf.add(butLower);
        buttonsInf.add(butRaise);
        buttonsInf.add(butTurn);
        buttonsInf.add(butMount);
        buttonsInf.add(butClimbMode);
        buttonsInf.add(butSearchlight);
        buttonsInf.add(butEject);
        buttonsInf.add(butFlee);
        buttonsInf.add(butRAC);
        buttonsInf.add(butLayMine);
        buttonsInf.add(butSwim);
        buttonsInf.add(butDigIn);
        buttonsInf.add(butFortify);
        buttonsInf.add(butClear);
        if ((clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()) != null) &&
                (clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()).getSize() > 1)) {
        buttonsInf.add(butForwardIni);
        }

        buttonsAero = new ArrayList<JButton>(24);
        if (!clientgui.getClient().game.useVectorMove()) {
            buttonsAero.add(butWalk);
            buttonsAero.add(butAcc);
            buttonsAero.add(butDec);
            buttonsAero.add(butTurn);
            buttonsAero.add(butLower);
            buttonsAero.add(butRaise);
            buttonsAero.add(butHover);
            buttonsAero.add(butRoll);
            buttonsAero.add(butEvadeAero);
            buttonsAero.add(butRam);
            buttonsAero.add(butManeuver);
            buttonsAero.add(butAccN);
            buttonsAero.add(butDecN);
            buttonsAero.add(butFlyOff);
            buttonsAero.add(butEject);
            buttonsAero.add(butLaunch);
            buttonsAero.add(butRecover);
            buttonsAero.add(butDrop);
            buttonsAero.add(butJoin);
            buttonsAero.add(butRAC);
            buttonsAero.add(butDump);
            buttonsAero.add(butLand);
            buttonsAero.add(butVLand);
        } else {
            buttonsAero.add(butThrust);
            buttonsAero.add(butTurnLeft);
            buttonsAero.add(butTurnRight);
            buttonsAero.add(butEvadeAero);
            buttonsAero.add(butRoll);
            buttonsAero.add(butYaw);
            buttonsAero.add(butEndOver);
            buttonsAero.add(butRam);
            buttonsAero.add(butEject);
            buttonsAero.add(butFlyOff);
            buttonsAero.add(butLaunch);
            buttonsAero.add(butRecover);
            buttonsAero.add(butDrop);
            buttonsAero.add(butJoin);
            buttonsAero.add(butRAC);
            buttonsAero.add(butDump);
            buttonsAero.add(butLand);
            buttonsAero.add(butVLand);
        }
        if ((clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()) != null) &&
                (clientgui.getClient().game.getTeamForPlayer(clientgui.getClient().getLocalPlayer()).getSize() > 1)) {
        buttonsAero.add(butForwardIni);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
            buttonsAero.add(butShutdown);
            buttonsAero.add(butStartup);
        }
        if (clientgui.getClient().game.getOptions().booleanOption("tacops_self_destruct")) {
            buttonsAero.add(butSelfDestruct);
        }

        // layout button grid
        panButtons = new JPanel();
        buttonLayout = 0;
        setupButtonPanel();

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        addBag(panButtons, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);
        clientgui.bv.addKeyListener(this);
        addKeyListener(this);
    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridBagLayout());

        // choose button order based on entity type
        ArrayList<JButton> buttonList = buttonsMech;
        final Entity ce = ce();
        if (ce != null) {
            if (ce instanceof Infantry) {
                buttonList = buttonsInf;
            } else if (ce instanceof VTOL) {
                buttonList = buttonsVtol;
            } else if (ce instanceof Tank) {
                buttonList = buttonsTank;
            } else if (ce instanceof Aero) {
                if (ce.isAirborne()) {
                    buttonList = buttonsAero;
                } else {
                    buttonList = buttonsTank;
                }
            }
        }
        // should this layout be skipped? (if nothing enabled)
        boolean ok = false;
        while (!ok && (buttonLayout != 0)) {
            for (int i = buttonLayout * 8; (i < ((buttonLayout + 1) * 8))
                    && (i < buttonList.size()); i++) {
                if (buttonList.get(i).isEnabled()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // skip as nothing was enabled
                buttonLayout++;
                if ((buttonLayout * 8) >= buttonList.size()) {
                    buttonLayout = 0;
                }
            }
        }
        int x = 0;
        int y = 0;
        panButtons.add(butNext, GBC.std().gridx(x).gridy(y).fill());

        x++;
        for (int i = buttonLayout * 8; (i < ((buttonLayout + 1) * 8))
                && (i < buttonList.size()); i++) {
            if (x == 5) {
                y++;
                x = 0;
            }
            panButtons.add(buttonList.get(i), GBC.std().gridx(x).gridy(y).fill());
            x++;
        }
        if(x == 5){
            y++;
            x = 0;
        }
        panButtons.add(butMore, GBC.std().gridx(4).gridy(1).fill());
        panButtons.add(butDone, GBC.std().gridx(5).gridy(0).gridheight(2).fill());
        panButtons.validate();
        panButtons.repaint();
    }

    /**
     * Hands over the current turn to the next valid player on the same team as the supplied player.
     * If no player on the team apart from this player has any turns left it activates this player again.
     */
    public synchronized void selectNextPlayer() {
        clientgui.getClient().sendNextPlayer();
        //endMyTurn();
    }


    /**
     * Selects an entity, by number, for movement.
     */
    public synchronized void selectEntity(int en) {
        final Entity ce = clientgui.getClient().game.getEntity(en);

        // hmm, sometimes this gets called when there's no ready entities?
        if (ce == null) {
            System.err
                    .println("MovementDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }
        cen = en;
        clientgui.setSelectedEntityNum(en);
        clear();
        updateButtons();
        // Update the menu bar.
        clientgui.getMenuBar().setEntity(ce);
        clientgui.getBoardView().highlight(ce.getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        clientgui.mechD.displayEntity(ce);
        clientgui.mechD.showPanel("movement"); //$NON-NLS-1$
        if (!clientgui.bv.isMovingUnits()) {
            clientgui.bv.centerOnHex(ce.getPosition());
        }
    }

    /**
     * Sets the buttons to their proper states
     */
    private void updateButtons() {
        final Entity ce = ce();
        boolean isMech = (ce instanceof Mech);
        boolean isInfantry = (ce instanceof Infantry);
        // boolean isProtomech = (ce instanceof Protomech);
        boolean isAero = (ce instanceof Aero);
        // ^-- I suppose these should really be methods, a-la
        // Entity.canCharge(), Entity.canDFA()...

        setWalkEnabled(!ce.isImmobile()
                && ((ce.getWalkMP() > 0) || (ce.getRunMP() > 0))
                && !ce.isStuck());
        setJumpEnabled(!isAero && !ce.isImmobile() && (ce.getJumpMP() > 0)
                && !(ce.isStuck() && !ce.canUnstickByJumping()));
        setSwimEnabled(!isAero
                && !ce.isImmobile()
                && ce.hasUMU()
                && ce.isUnderwater());
        setBackUpEnabled(!isAero && butWalk.isEnabled());
        setChargeEnabled(ce.canCharge());
        setDFAEnabled(ce.canDFA());
        setRamEnabled(ce.canRam());

        if (isInfantry) {
            if (clientgui.getClient().game.containsMinefield(ce.getPosition())) {
                setClearEnabled(true);
            } else {
                setClearEnabled(false);
            }
        } else {
            setClearEnabled(false);
        }
        if ((ce.getMovementMode() == EntityMovementMode.HYDROFOIL)
                || (ce.getMovementMode() == EntityMovementMode.NAVAL)
                || (ce.getMovementMode() == EntityMovementMode.SUBMARINE)
                || (ce.getMovementMode() == EntityMovementMode.INF_UMU)
                || (ce.getMovementMode() == EntityMovementMode.VTOL)
                || (ce.getMovementMode() == EntityMovementMode.BIPED_SWIM)
                || (ce.getMovementMode() == EntityMovementMode.QUAD_SWIM)) {
            butClimbMode.setEnabled(false);
        } else {
            butClimbMode.setEnabled(true);
        }
        if (ce instanceof Infantry) {
            butDigIn.setEnabled(true);
            butFortify.setEnabled(true);
        } else {
            butDigIn.setEnabled(false);
            butFortify.setEnabled(false);
        }
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
        updateDumpButton();
        updateEvadeButton();
        
        updateStartupButton();
        updateShutdownButton();

        if (ce instanceof Aero) {
            butThrust.setEnabled(true);
            butYaw.setEnabled(true);
            butEndOver.setEnabled(true);
            butTurnLeft.setEnabled(true);
            butTurnRight.setEnabled(true);
            setEvadeAeroEnabled(true);
            setEjectEnabled(true);
            // no turning for spheroids in atmosphere
            if ((((Aero) ce).isSpheroid() || clientgui.getClient().game
                    .getPlanetaryConditions().isVacuum())
                    && !clientgui.getClient().game.getBoard().inSpace()) {
                setTurnEnabled(false);
            }
        }

        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();
        updateFlyOffButton();
        updateLaunchButton();
        updateDropButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();

        if (isInfantry
                && ce.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_VIBROSHOVEL)) {
            butFortify.setEnabled(true);
        } else {
            butFortify.setEnabled(false);
        }
        if (isInfantry
                && clientgui.getClient().game.getOptions().booleanOption(
                        "tacops_dig_in")) {
            butDigIn.setEnabled(true);
        } else {
            butDigIn.setEnabled(false);
        }
        butShakeOff.setEnabled((ce instanceof Tank)
                && (ce.getSwarmAttackerId() != Entity.NONE));

        setLayMineEnabled(ce.canLayMine());
        setFleeEnabled(ce.canFlee());
        if (clientgui.getClient().game.getOptions().booleanOption(
                "vehicles_can_eject")) { //$NON-NLS-1$
            setEjectEnabled((!isInfantry)
                    && !(isMech && (((Mech) ce).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED))
                    && ce.isActive());
        } else {
            setEjectEnabled(isMech
                    && (((Mech) ce).getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)
                    && ce.isActive()
                    && !ce.hasQuirk("no_eject"));
        }

        if(ce.isDropping()) {
            disableButtons();
            butDone.setEnabled(true);
        }

        //if small craft/dropship that has unloaded units, then only allowed
        //to unload more
        if(ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
        }

        setupButtonPanel();
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getString("MovementDisplay.its_your_turn")); //$NON-NLS-1$
        butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Done")+"</b></html>"); //$NON-NLS-1$
        butDone.setEnabled(true);
        setNextEnabled(true);
        setForwardIniEnabled(true);
        butMore.setEnabled(true);
        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }
        selectEntity(clientgui.getClient().getFirstEntityNum());
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private synchronized void endMyTurn() {
        final Entity ce = ce();

        // end my turn, then.
        disableButtons();
        Entity next = clientgui.getClient().game.getNextEntity(clientgui
                .getClient().game.getTurnIndex());
        if ((IGame.Phase.PHASE_MOVEMENT == clientgui.getClient().game
                .getPhase())
                && (null != next)
                && (null != ce)
                && (next.getOwnerId() != ce.getOwnerId())) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.clearMovementData();
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
        butMore.setEnabled(false);
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setMountEnabled(false);
        setUnloadEnabled(false);
        setClearEnabled(false);
        setHullDownEnabled(false);
        setSwimEnabled(false);
        setAccEnabled(false);
        setDecEnabled(false);
        setEvadeEnabled(false);
        setShutdownEnabled(false);
        setStartupEnabled(false);
        setSelfDestructEnabled(false);
        setEvadeAeroEnabled(false);
        setAccNEnabled(false);
        setDecNEnabled(false);
        setRollEnabled(false);
        setLaunchEnabled(false);
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
        setRecklessEnabled(false);
        setGoProneEnabled(false);
        butClimbMode.setEnabled(false);
        butDigIn.setEnabled(false);
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
        
        if (ce == null){
            return;
        }
        
        // switch back from swimming to normal mode.
        if (ce.getMovementMode() == EntityMovementMode.BIPED_SWIM) {
            ce.setMovementMode(EntityMovementMode.BIPED);
        } else if (ce.getMovementMode() == EntityMovementMode.QUAD_SWIM) {
            ce.setMovementMode(EntityMovementMode.QUAD);
        }

        // create new current and considered paths
        cmd = new MovePath(clientgui.getClient().game, ce);

        // set to "walk," or the equivalent
        gear = MovementDisplay.GEAR_LAND;

        // update some GUI elements
        clientgui.bv.clearMovementData();
        butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Done")+"</b></html>"); //$NON-NLS-1$
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateElevationButtons();
        updateTakeOffButtons();
        updateLandButtons();
        updateFlyOffButton();
        updateLaunchButton();
        updateDropButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();

        loadedUnits = ce.getLoadedUnits();
        if(ce instanceof Aero) {
            loadedUnits = ce.getUnitsUnloadableFromBays();
        }

        updateLoadButtons();
        updateJoinButton();
        updateRecoveryButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();

        //if dropping unit only allow turning
        if(ce.isDropping()) {
            gear = MovementDisplay.GEAR_TURN;
            disableButtons();
            butDone.setEnabled(true);
        }

        //if small craft/dropship that has unloaded units, then only allowed
        //to unload more
        if(ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }
    }

    private void removeLastStep() {
        cmd.removeLastStep();
        if (cmd.length() == 0) {
            clear();
        } else {
            clientgui.bv.drawMovementData(ce(), cmd);

            // Set the button's label to "Done"
            // if the entire move is impossible.
            MovePath possible = cmd.clone();
            possible.clipToPossible();
            if (possible.length() == 0) {
                butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Done")+"</b></html>"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    @Override
    public synchronized void ready() {

        cmd.clipToPossible();
        if ((cmd.length() == 0) && !ce().isAirborne()
                && GUIPreferences.getInstance().getNagForNoAction()) {
            // Hmm....no movement steps, comfirm this action
            String title = Messages
                    .getString("MovementDisplay.ConfirmNoMoveDlg.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("MovementDisplay.ConfirmNoMoveDlg.message"); //$NON-NLS-1$
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
            if (!((ce() instanceof VTOL) && ce().hasWorkingMisc(MiscType.F_JET_BOOSTER))) {
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame,
                        Messages.getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                        Messages
                                .getString(
                                        "MovementDisplay.ConfirmMoveRoll", new Object[] { new Integer(ce().getMASCTarget()) }), //$NON-NLS-1$
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

        if ((cmd.getLastStepMovementType() == EntityMovementType.MOVE_SPRINT)
                && GUIPreferences.getInstance().getNagForSprint()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                    .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
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
        if ((check.length() > 0) && GUIPreferences.getInstance().getNagForPSR()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                    .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ConfirmPilotingRoll") + //$NON-NLS-1$
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

        // check for G-forces
        check = SharedUtility.doThrustCheck(cmd, clientgui.getClient());
        if ((check.length() > 0) && GUIPreferences.getInstance().getNagForPSR()) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                    .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ConfirmPilotingRoll") + //$NON-NLS-1$
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

        //check for unsafe takeoffs
        if(cmd.contains(MoveStepType.VTAKEOFF) || cmd.contains(MoveStepType.TAKEOFF)) {
            boolean unsecure = false;
            for(Entity loaded : ce().getLoadedUnits()) {
                if(loaded.wasLoadedThisTurn() && !(loaded instanceof Infantry)) {
                    unsecure = true;
                    break;
                }
            }
            if(unsecure) {
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                        .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
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


        //check to see if spheroids will drop an elevation
        if((ce() instanceof Aero) && ((Aero)ce()).isSpheroid() && !clientgui.getClient().game.getBoard().inSpace()
                && ((Aero)ce()).isAirborne()
                && (cmd.getFinalNDown()==0) && (cmd.getMpUsed()==0) && !cmd.contains(MoveStepType.VLAND)) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                    .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.SpheroidAltitudeLoss") + //$NON-NLS-1$
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

        if (ce().isAirborne() && (ce() instanceof Aero)) {
            if (!clientgui.getClient().game.useVectorMove()
                    && !((Aero) ce()).isOutControlTotal()) {
                // check for underuse of velocity
                boolean unusedVelocity = false;
                if (null != cmd.getLastStep()) {
                    unusedVelocity = cmd.getLastStep().getVelocityLeft() > 0;
                } else {
                    unusedVelocity = ((Aero) ce()).getCurrentVelocity() > 0;
                }
                boolean flyoff = false;
                if ((null != cmd) && (cmd.contains(MoveStepType.OFF) || cmd.contains(MoveStepType.RETURN))) {
                    flyoff = true;
                }
                boolean landing = false;
                if ((null != cmd) && cmd.contains(MoveStepType.LAND)) {
                    landing = true;
                }
                if (unusedVelocity && !flyoff && !landing) {
                    String title = Messages
                            .getString("MovementDisplay.VelocityLeft.title"); //$NON-NLS-1$
                    String body = Messages
                            .getString("MovementDisplay.VelocityLeft.message"); //$NON-NLS-1$
                    clientgui.doAlertDialog(title, body);
                    return;
                }
            }
            // depending on the rules and location (i.e. space v. atmosphere),
            // Aeros might need to have additional move steps tacked on
            // This must be done after all prompts, otherwise a user who cancels
            // will still have steps added to the movepath.
            cmd = SharedUtility.moveAero(cmd, clientgui.getClient());
        }

        disableButtons();
        clientgui.bv.clearMovementData();
        if (ce().hasUMU()) {
            clientgui.getClient().sendUpdateEntity(ce());
        }
        clientgui.getClient().moveEntity(cen, cmd);
    }

    /**
     * Returns the current entity.
     */
    private synchronized Entity ce() {
        return clientgui.getClient().game.getEntity(cen);
    }

    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if (shiftheld || (gear == GEAR_TURN)) {
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false);
        } else if ((gear == GEAR_LAND) || (gear == GEAR_JUMP)) {
            cmd.findPathTo(dest, MoveStepType.FORWARDS);
        } else if (gear == GEAR_BACKUP) {
            cmd.findPathTo(dest, MoveStepType.BACKWARDS);
        } else if (gear == GEAR_CHARGE) {
            cmd.findPathTo(dest, MoveStepType.CHARGE);
        } else if (gear == GEAR_DFA) {
            cmd.findPathTo(dest, MoveStepType.DFA);
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
    }

    //
    // BoardListener
    //
    @Override
    public synchronized void hexMoused(BoardViewEvent b) {
        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // don't make a movement path for aeros if advanced movement is on
        boolean nopath = (ce instanceof Aero)
                && clientgui.getClient().game.useVectorMove();

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn()
                || ((b.getModifiers() & InputEvent.BUTTON1_MASK) == 0)) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_MASK) != 0)
                || ((b.getModifiers() & InputEvent.ALT_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;
        }
        if ((b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && !nopath) {
            if (!b.getCoords().equals(clientgui.getBoardView().getLastCursor())
                    || shiftheld || (gear == MovementDisplay.GEAR_TURN)) {
                clientgui.getBoardView().cursor(b.getCoords());

                // either turn or move
                if (ce != null) {
                    currentMove(b.getCoords());
                    clientgui.bv.drawMovementData(ce(), cmd);
                }
            }
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            Coords moveto = b.getCoords();
            clientgui.bv.drawMovementData(ce(), cmd);
            clientgui.getBoardView().select(b.getCoords());
            if (shiftheld || (gear == MovementDisplay.GEAR_TURN)) {
                butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Move")+"</b></html>"); //$NON-NLS-1$

                // Set the button's label to "Done"
                // if the entire move is impossible.
                MovePath possible = cmd.clone();
                possible.clipToPossible();
                if (possible.length() == 0) {
                    butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Done")+"</b></html>"); //$NON-NLS-1$
                }
                return;
            }
            if (gear == MovementDisplay.GEAR_RAM) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)
                        || !(target instanceof Aero)) {
                    clientgui
                            .doAlertDialog(
                                    Messages
                                            .getString("MovementDisplay.CantRam"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    return;
                }

                // check if it's a valid ram
                // First I need to add moves to the path if advanced
                if ((ce instanceof Aero)
                        && clientgui.getClient().game.useVectorMove()) {
                    cmd.clipToPossible();
                    // cmd = addSteps(cmd, ce, clientgui.getClient());
                }

                cmd.addStep(MoveStepType.RAM);

                ToHitData toHit = new RamAttackAction(cen, target
                        .getTargetType(), target.getTargetId(), target
                        .getPosition()).toHit(clientgui.getClient().game, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {

                    // Determine how much damage the charger will take.
                    Aero ta = (Aero) target;
                    Aero ae = (Aero) ce;
                    int toAttacker = RamAttackAction.getDamageTakenBy(ae, ta,
                            cmd.getSecondFinalPosition(ae.getPosition()), cmd
                                    .getHexesMoved(), ta.getCurrentVelocity());
                    int toDefender = RamAttackAction.getDamageFor(ae, ta, cmd
                            .getSecondFinalPosition(ae.getPosition()), cmd
                            .getHexesMoved(), ta.getCurrentVelocity());

                    // Ask the player if they want to charge.
                    if (clientgui
                            .doYesNoDialog(
                                    Messages
                                            .getString(
                                                    "MovementDisplay.RamDialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                                    Messages
                                            .getString(
                                                    "MovementDisplay.RamDialog.message", new Object[] { //$NON-NLS-1$
                                                            toHit
                                                                    .getValueAsString(),
                                                            new Double(
                                                                    Compute
                                                                            .oddsAbove(toHit
                                                                                    .getValue())),
                                                            toHit.getDesc(),
                                                            new Integer(
                                                                    toDefender),
                                                            toHit
                                                                    .getTableDesc(),
                                                            new Integer(
                                                                    toAttacker) }))) {
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
                clientgui.doAlertDialog(Messages
                        .getString("MovementDisplay.CantRam"), //$NON-NLS-1$
                        toHit.getDesc());
                clear();
                return;
            } else if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui
                            .doAlertDialog(
                                    Messages
                                            .getString("MovementDisplay.CantCharge"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    return;
                }

                // check if it's a valid charge
                ToHitData toHit = new ChargeAttackAction(cen, target
                        .getTargetType(), target.getTargetId(), target
                        .getPosition()).toHit(clientgui.getClient().game, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // Determine how much damage the charger will take.
                    int toAttacker = 0;
                    if (target.getTargetType() == Targetable.TYPE_ENTITY) {
                        Entity te = (Entity) target;
                        toAttacker = ChargeAttackAction
                                .getDamageTakenBy(
                                        ce,
                                        te,
                                        clientgui.getClient().game.getOptions()
                                                .booleanOption(
                                                        "tacops_charge_damage"), cmd.getHexesMoved()); //$NON-NLS-1$
                    } else if ((target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                            || (target.getTargetType() == Targetable.TYPE_BUILDING)) {
                        Building bldg = clientgui.getClient().game.getBoard()
                                .getBuildingAt(moveto);
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce,
                                bldg, moveto);
                    }

                    // Ask the player if they want to charge.
                    if (clientgui
                            .doYesNoDialog(
                                    Messages
                                            .getString(
                                                    "MovementDisplay.ChargeDialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                                    Messages
                                            .getString(
                                                    "MovementDisplay.ChargeDialog.message", new Object[] {//$NON-NLS-1$
                                                            toHit
                                                                    .getValueAsString(),
                                                            new Double(
                                                                    Compute
                                                                            .oddsAbove(toHit
                                                                                    .getValue())),
                                                            toHit.getDesc(),
                                                            new Integer(
                                                                    ChargeAttackAction
                                                                            .getDamageFor(
                                                                                    ce,
                                                                                    clientgui
                                                                                            .getClient().game
                                                                                            .getOptions()
                                                                                            .booleanOption(
                                                                                                    "tacops_charge_damage"),
                                                                                    cmd
                                                                                            .getHexesMoved())),
                                                            toHit
                                                                    .getTableDesc(),
                                                            new Integer(
                                                                    toAttacker) }))) {
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
                clientgui.doAlertDialog(Messages
                        .getString("MovementDisplay.CantCharge"), //$NON-NLS-1$
                        toHit.getDesc());
                clear();
                return;
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui
                            .doAlertDialog(
                                    Messages
                                            .getString("MovementDisplay.CantDFA"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = DfaAttackAction.toHit(
                        clientgui.getClient().game, cen, target, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if (clientgui
                            .doYesNoDialog(
                                    Messages
                                            .getString(
                                                    "MovementDisplay.DFADialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                                    Messages
                                            .getString(
                                                    "MovementDisplay.DFADialog.message", new Object[] {//$NON-NLS-1$
                                                            toHit
                                                                    .getValueAsString(),
                                                            new Double(
                                                                    Compute
                                                                            .oddsAbove(toHit
                                                                                    .getValue())),
                                                            toHit.getDesc(),
                                                            new Integer(
                                                                    DfaAttackAction
                                                                            .getDamageFor(
                                                                                    ce,
                                                                                    (target instanceof Infantry)
                                                                                            && !(target instanceof BattleArmor))),
                                                            toHit
                                                                    .getTableDesc(),
                                                            new Integer(
                                                                    DfaAttackAction
                                                                            .getDamageTakenBy(ce)) }))) {
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
                clientgui.doAlertDialog(Messages
                        .getString("MovementDisplay.CantDFA"), //$NON-NLS-1$
                        toHit.getDesc());
                clear();
                return;
            }
            butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Move")+"</b></html>"); //$NON-NLS-1$
            updateProneButtons();
            updateRACButton();
            updateSearchlightButton();
            updateLoadButtons();
            updateElevationButtons();
            updateTakeOffButtons();
            updateLandButtons();
            updateEvadeButton();
            updateShutdownButton();
            updateStartupButton();
            updateSelfDestructButton();
            updateFlyOffButton();
            updateLaunchButton();
            updateDropButton();
            updateRecklessButton();
            updateHoverButton();
            updateManeuverButton();
            updateSpeedButtons();
            updateThrustButton();
            updateRollButton();
            updateTurnButton();
            checkFuel();
            checkOOC();
            checkAtmosphere();
        }
    }

    private synchronized void updateProneButtons() {
        final Entity ce = ce();
        if (ce != null) {
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
                        && !(butUp.isEnabled()));
                if (!(ce instanceof Tank)){
                    setHullDownEnabled(ce.canGoHullDown());
                }else{
                    // So that vehicle can move and go hull-down, we have to
                    //  check if it's moved into a fortified position
                    if (cmd.getLastStep() != null){
                        boolean hullDownEnabled = clientgui.getClient().game.
                            getOptions().booleanOption("tacops_hull_down");
                        IHex occupiedHex = clientgui.getClient().game.
                            getBoard().getHex(cmd.getLastStep().getPosition());
                        boolean fortifiedHex = 
                            occupiedHex.containsTerrain(Terrains.FORTIFIED); 
                        setHullDownEnabled(hullDownEnabled && fortifiedHex);
                    }else{
                        // If there's queued up movement, we can call the
                        //  canGoHullDown() method in the Tank class.
                        setHullDownEnabled(ce.canGoHullDown()); 
                    }
                    
                }
            }
        } else {
            setGetUpEnabled(false);
            setGoProneEnabled(false);
            setHullDownEnabled(false);
        }
    }

    private void updateRACButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }
        setUnjamEnabled(ce.canUnjamRAC()
                && ((gear == MovementDisplay.GEAR_LAND)
                        || (gear == MovementDisplay.GEAR_TURN) || (gear == MovementDisplay.GEAR_BACKUP))
                && ((cmd.getMpUsed() <= ce.getWalkMP()) || (cmd.getLastStep().isOnlyPavement() && (cmd.getMpUsed() <= (ce.getWalkMP() + 1)))));
    }

    private void updateSearchlightButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }
        setSearchlightEnabled(ce.hasSpotlight()
                && !cmd.contains(MoveStepType.SEARCHLIGHT), ce()
                .isUsingSpotlight());
    }

    private synchronized void updateElevationButtons() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (ce.isAirborne()) {
            // then use altitude not elevation
            setRaiseEnabled(ce.canGoUp(cmd.getFinalAltitude(), cmd
                    .getFinalCoords()));
            setLowerEnabled(ce.canGoDown(cmd.getFinalAltitude(), cmd
                    .getFinalCoords()));
            return;
        }
        setRaiseEnabled(ce.canGoUp(cmd.getFinalElevation(), cmd
                .getFinalCoords()));
        setLowerEnabled(ce.canGoDown(cmd.getFinalElevation(), cmd
                .getFinalCoords()));
    }

    private synchronized void updateTakeOffButtons() {

        if((null != cmd) && (cmd.length() > 0)) {
            //you can't take off if you have already moved
            //http://www.classicbattletech.com/forums/index.php?topic=54112.0
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
            return;
        }

        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if(ce instanceof Aero) {
            if(ce.isAirborne()) {
                setTakeOffEnabled(false);
                setVTakeOffEnabled(false);
            } else  if (!ce.isShutDown()){
                setTakeOffEnabled(((Aero)ce).canTakeOffHorizontally());
                setVTakeOffEnabled(((Aero)ce).canTakeOffVertically());
            }
        } else {
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
        }
    }

    private synchronized void updateLandButtons() {


    	if((null != cmd) && (cmd.length() > 0)) {
            //According to the link below, you can move in the air and then land
    		//but I have personal message to Welshman right now asking that this be changed
    		//because it creates all kinds of rules problems, the number one being
    		//the ability to use spheroid dropships to perform insta-Death From Above attacks
    		//that cannot be defended against
    		//So we are going to disallow it
            //http://www.classicbattletech.com/forums/index.php?topic=54112.0
            setLandEnabled(false);
            return;
        }

        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        //only allow landing on the ground map not atmosphere or space map
        if(!clientgui.getClient().game.getBoard().onGround()) {
            return;
        }

        if(ce instanceof Aero) {
            if(ce.isAirborne() && (cmd.getFinalAltitude() == 1)) {
               setLandEnabled(((Aero)ce).canLandHorizontally());
               setVLandEnabled(((Aero)ce).canLandVertically());
            }
        }

    }

    private void updateRollButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        setRollEnabled(true);

        if (!clientgui.getClient().game.getBoard().inSpace()) {
            setRollEnabled(false);
        }

        if (cmd.contains(MoveStepType.ROLL)) {
            setRollEnabled(false);
        }

        return;

    }

    private void updateHoverButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        if (!a.isVSTOL()) {
            return;
        }

        if (clientgui.getClient().game.getBoard().inSpace()) {
            return;
        }

        if (!cmd.contains(MoveStepType.HOVER)) {
            setHoverEnabled(true);
        } else {
            setHoverEnabled(false);
        }

        return;

    }

    private void updateThrustButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        // only allow thrust if there is thrust left to spend
        int mpUsed = 0;
        MoveStep last = cmd.getLastStep();
        if (null != last) {
            mpUsed = last.getMpUsed();
        }

        if (((ce instanceof FighterSquadron) && (mpUsed >= ((FighterSquadron)ce).getWalkMP())) || (!(ce instanceof FighterSquadron) && (mpUsed >= a.getRunMP()))) {
            setThrustEnabled(false);
        } else {
            setThrustEnabled(true);
        }

        return;
    }

    private synchronized void updateSpeedButtons() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        // only allow acceleration and deceleration if the cmd is empty or the
        // last step was
        // acc/dec

        setAccEnabled(false);
        setDecEnabled(false);
        MoveStep last = cmd.getLastStep();
        // figure out implied velocity so you can't decelerate below zero
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
        if (!cmd.contains(MoveStepType.ACC)
                && !cmd.contains(MoveStepType.DEC)
                && !cmd.contains(MoveStepType.DECN)) {
            setAccNEnabled(true);
        }

        if (!cmd.contains(MoveStepType.ACC)
                && !cmd.contains(MoveStepType.DEC)
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
        if (!clientgui.getClient().game.getBoard().inSpace()
                && (vel == (2 * a.getWalkMP()))) {
            setAccEnabled(false);
        }
        // velocity next will get halved before next turn so allow up to 4 times
        if (!clientgui.getClient().game.getBoard().inSpace()
                && (veln == (4 * a.getWalkMP()))) {
            setAccNEnabled(false);
        }

        // if this is a tele-operated missile then it can't decelerate no matter
        // what
        if (a instanceof TeleMissile) {
            setDecEnabled(false);
            setDecNEnabled(false);
        }

        return;
    }

    private void updateDumpButton() {
        /*
         * if (ce() instanceof Aero) { Aero a = (Aero) ce();
         * setDumpEnabled(a.hasBombs() && cmd.length() == 0); } else {
         * setDumpEnabled(false); }
         */
    }

    private void updateFlyOffButton() {
        final Entity ce = ce();

        // Aeros should be able to fly off if they reach a border hex with
        // velocity
        // remaining
        // and facing the right direction
        if (!(ce instanceof Aero)) {
            setFlyOffEnabled(false);
            return;
        }
        if(!ce.isAirborne()) {
            setFlyOffEnabled(false);
            return;
        }

        Aero a = (Aero) ce;
        MoveStep step = cmd.getLastStep();
        Coords position = ce.getPosition();
        int facing = ce.getFacing();

        int velocityLeft = a.getCurrentVelocity();
        if (step != null) {
            position = step.getPosition();
            facing = step.getFacing();
            velocityLeft = step.getVelocityLeft();
        }

        //for spheroids in atmosphere we just need to check being on the edge
        if(a.isSpheroid() && !clientgui.getClient().game.getBoard().inSpace()) {
            setFlyOffEnabled((position != null) && (a.getWalkMP() > 0) && ((position.x == 0) || (position.x == (clientgui.getClient().game.getBoard().getWidth() - 1)) || (position.y == 0) || (position.y == (clientgui.getClient().game.getBoard().getHeight() - 1))));
            return;
        }

        //for all aerodynes and spheroids in space it is more complicated - the nose of the aircraft
        //must be facing in the righ direction and there must be velocity remaining

        boolean evenx = (position.x % 2) == 0;
        if ((velocityLeft > 0)
                && (((position.x == 0) && ((facing == 5) || (facing == 4)))
                        || ((position.x == (clientgui.getClient().game
                                .getBoard().getWidth() - 1)) && ((facing == 1) || (facing == 2)))
                        || ((position.y == 0)
                                && ((facing == 1) || (facing == 5) || (facing == 0)) && evenx)
                        || ((position.y == 0) && (facing == 0))
                        || ((position.y == (clientgui.getClient().game
                                .getBoard().getHeight() - 1))
                                && ((facing == 2) || (facing == 3) || (facing == 4)) && !evenx) || ((position.y == (clientgui
                        .getClient().game.getBoard().getHeight() - 1)) && (facing == 3)))) {
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

        setLaunchEnabled((ce.getLaunchableFighters().size() > 0)
                || (ce.getLaunchableSmallCraft().size() > 0));

    }

    private void updateDropButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        setDropEnabled(ce.isAirborne()
                && (ce.getDroppableUnits().size() > 0));

    }

    private void updateEvadeButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().game.getOptions().booleanOption(
                "tacops_evade")) {
            return;
        }

        if (!((ce instanceof Mech) || (ce instanceof Tank))) {
            return;
        }

        setEvadeEnabled((cmd.getLastStepMovementType() != EntityMovementType.MOVE_JUMP)
                && (cmd.getLastStepMovementType() != EntityMovementType.MOVE_SPRINT));
    }

    private void updateShutdownButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (!clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
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

        if (!clientgui.getClient().game.getOptions().booleanOption("manual_shutdown")) {
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

        if (!clientgui.getClient().game.getOptions().booleanOption("tacops_self_destruct")) {
            return;
        }

        if (ce instanceof Infantry) {
            return;
        }

        setSelfDestructEnabled(ce.getEngine().isFusion() && !ce.getSelfDestructing() && !ce.getSelfDestructInitiated());
    }

    private void updateRecklessButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (ce instanceof Protomech) {
            setRecklessEnabled(false);
        } else {
            setRecklessEnabled((null == cmd) || (cmd.length() == 0));
        }
    }

    private void updateManeuverButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if (clientgui.getClient().game.getBoard().inSpace()) {
            return;
        }

        if (!ce.isAirborne()) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

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

    private synchronized void updateLoadButtons() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if(ce instanceof SmallCraft) {
            setUnloadEnabled((ce.getUnitsUnloadableFromBays().size() > 0) && !ce.isAirborne());
            setLoadEnabled(false);
            return;
        }

        //can this unit mount a dropship/small craft?
        setMountEnabled(false);
        Coords pos = ce.getPosition();
        int elev = ce.getElevation();
        int mpUsed = ce.mpUsed;
        if(null != cmd) {
            pos = cmd.getFinalCoords();
            elev = cmd.getFinalElevation();
            mpUsed = cmd.getMpUsed();
        }
        if(!ce.isAirborne() && (mpUsed <= Math.ceil((ce.getWalkMP() / 2.0)))
                && (Compute.getMountableUnits(ce, pos, elev, clientgui.getClient().game).size() > 0)) {
            setMountEnabled(true);
        }

        boolean legalGear = ((gear == MovementDisplay.GEAR_LAND)
                || (gear == MovementDisplay.GEAR_TURN) || (gear == MovementDisplay.GEAR_BACKUP));
        int unloadEl = cmd.getFinalElevation();
        IHex hex = ce.getGame().getBoard().getHex(cmd.getFinalCoords());
        boolean canUnloadHere = false;
        for (Entity en : loadedUnits) {
            if (en.isElevationValid(unloadEl, hex) || (en.getJumpMP() > 0)) {
                canUnloadHere = true;
                break;
            }
        }
        // Disable the "Unload" button if we're in the wrong
        // gear or if the entity is not transporting units.
        if (!legalGear || (loadedUnits.size() == 0) || (cen == Entity.NONE)
                || (!canUnloadHere)) {
            setUnloadEnabled(false);
        } else {
            setUnloadEnabled(true);
        }
        // If the current entity has moved, disable "Load" button.
        if ((cmd.length() > 0) || (cen == Entity.NONE)) {
            setLoadEnabled(false);
        } else {
            // Check the other entities in the current hex for friendly units.
            Entity other = null;
            Enumeration<Entity> entities = clientgui.getClient().game
                    .getEntities(ce.getPosition());
            boolean isGood = false;
            while (entities.hasMoreElements()) {
                other = entities.nextElement();
                // If the other unit is friendly and not the current entity
                // and the current entity has at least 1 MP, if it can
                    // transport the other unit, and if the other hasn't moved
                    // then enable the "Load" button.
                    if ((ce.getWalkMP() > 0) && ce.canLoad(other)
                            && other.isLoadableThisTurn()) {
                        setLoadEnabled(true);
                        isGood = true;

                    // We can stop looking.
                    break;
                }
                // Nope. Discard it.
                other = null;
            } // Check the next entity in this position.
            if (!isGood) {
                setLoadEnabled(false);
            }
        } // End ce-hasn't-moved
    } // private void updateLoadButtons

    private Entity getMountedUnit() {
        Entity ce = ce();
        Entity choice = null;
        Coords pos = ce.getPosition();
        int elev = ce.getElevation();
        if(null != cmd) {
            pos = cmd.getFinalCoords();
            elev = cmd.getFinalElevation();
        }
        IHex hex = clientgui.getClient().game.getBoard().getHex(pos);
        if(null != hex) {
            elev += hex.getElevation();
        }

        ArrayList<Entity> mountableUnits = Compute.getMountableUnits(ce, pos, elev, clientgui.getClient().game);

     // Handle error condition.
        if (mountableUnits.size() == 0) {
            System.err
                    .println("MovementDisplay#getMountedUnit() called without mountable units."); //$NON-NLS-1$
        }

        // If we have multiple choices, display a selection dialog.
        else if (mountableUnits.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages
                                    .getString(
                                            "MovementDisplay.MountUnitDialog.message", new Object[] {//$NON-NLS-1$
                                            ce.getShortName()}),
                            Messages
                                    .getString("MovementDisplay.MountUnitDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(mountableUnits), null);
            choice = (Entity) SharedUtility.getTargetPicked(mountableUnits, input);
        } // End have-choices

        // Only one choice.
        else {
            choice = mountableUnits.get(0);
        }

        // Return the chosen unit.
        return choice;
    }

    /**
     * Get the unit that the player wants to unload. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     *         value will not be <code>null</code>.
     */
    private Entity getUnloadedUnit() {
        Entity ce = ce();
        Entity choice = null;
        // Handle error condition.
        if (loadedUnits.size() == 0) {
            System.err
                    .println("MovementDisplay#getUnloadedUnit() called without loaded units."); //$NON-NLS-1$
        }

        // If we have multiple choices, display a selection dialog.
        else if (loadedUnits.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages
                                    .getString(
                                            "MovementDisplay.UnloadUnitDialog.message", new Object[] {//$NON-NLS-1$
                                            ce.getShortName(),
                                                    ce.getUnusedString() }),
                            Messages
                                    .getString("MovementDisplay.UnloadUnitDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(loadedUnits), null);
            choice = (Entity) SharedUtility.getTargetPicked(loadedUnits, input);
        } // End have-choices

        // Only one choice.
        else {
            choice = loadedUnits.get(0);
            loadedUnits.remove(0);
        }

        // Return the chosen unit.
        return choice;
    }

    private Coords getUnloadPosition(Entity unloaded) {
        Entity ce = ce();
        //we need to allow the user to select a hex for offloading
        Coords pos = ce.getPosition();
        if(null != cmd) {
            pos = cmd.getFinalCoords();
        }
        int elev = clientgui.getClient().game.getBoard().getHex(pos).getElevation() + ce.getElevation();
        ArrayList<Coords> ring = Compute.coordsAtRange(pos, 1);
        if(ce instanceof Dropship) {
            ring = Compute.coordsAtRange(pos, 2);
        }
        //ok now we need to go through the ring and identify available Positions
        ring = Compute.getAcceptableUnloadPositions(ring, unloaded, clientgui.getClient().game, elev);
        if(ring.size() < 1) {
            String title = Messages.getString("MovementDisplay.NoPlaceToUnload.title"); //$NON-NLS-1$
            String body = Messages.getString("MovementDisplay.NoPlaceToUnload.message"); //$NON-NLS-1$
            clientgui.doAlertDialog(title, body);
            return null;
        }
        String[] choices = new String[ring.size()];
        int i = 0;
        for(Coords c : ring) {
            choices[i++] = c.toString();
        }
        String selected = (String) JOptionPane
        .showInputDialog(
                clientgui,
                Messages
                        .getString(
                                "MovementDisplay.ChooseHex.message", new Object[] {//$NON-NLS-1$
                                ce.getShortName(),
                                        ce.getUnusedString() }),
                Messages
                        .getString("MovementDisplay.ChooseHex.title"), //$NON-NLS-1$
                JOptionPane.QUESTION_MESSAGE, null, choices, null);
        Coords choice = null;
        if (selected == null) {
            return choice;
        }
        for(Coords c : ring) {
            if(selected.equals(c.toString())) {
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

        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        // I also want to handle fighter recovery here. If using advanced
        // movement
        // it is not a function of where carrier is but where the carrier will
        // be at the end
        // of its move
        if (ce instanceof Aero) {
            Coords loadeePos = cmd.getFinalCoords();
            if (clientgui.getClient().game.useVectorMove()) {
                // not where you are, but where you will be
                loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd
                        .getFinalVectors());
            }
            Entity other = null;
            Enumeration<Entity> entities = clientgui.getClient().game
                    .getEntities(loadeePos);
            boolean isGood = false;
            while (entities.hasMoreElements()) {
                other = entities.nextElement();
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
                        if (clientgui.getClient().game.useVectorMove()) {
                            if (Compute.sameVectors(cmd.getFinalVectors(), oa
                                    .getVectors())) {
                                setRecoverEnabled(true);
                                isGood = true;

                            // We can stop looking now.
                            break;
                            }
                        } else if (cmd.getFinalVelocity() == oa
                                .getCurrentVelocity()) {
                            setRecoverEnabled(true);
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
            }
        }

    }

    /**
     * Joining a squadron - Similar to fighter recovery. You can fly up and join
     * a squadron or another solo fighter
     */
    private synchronized void updateJoinButton() {

        if (!clientgui.getClient().game.getOptions().booleanOption(
                "stratops_capital_fighter")) {
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
        if (clientgui.getClient().game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd
                    .getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = clientgui.getClient().game
                .getEntities(loadeePos);
        boolean isGood = false;
        while (entities.hasMoreElements()) {
            other = entities.nextElement();
            // Is the other unit friendly and not the current entity?
                // must be done with its movement
                // it also must be same heading and velocity
            if (ce.getOwner().equals(other.getOwner()) && other.isCapitalFighter() && other.isDone()
                        && other.canLoad(ce)
                        && (cmd.getFinalFacing() == other.getFacing())) {
                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (clientgui.getClient().game.useVectorMove()) {
                        // can you do equality with vectors?
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa
                                .getVectors())) {
                            setJoinEnabled(true);
                            isGood = true;

                        // We're done looping now...
                        break;
                        }
                    } else if (cmd.getFinalVelocity() == oa
                            .getCurrentVelocity()) {
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
     *         value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getLaunchedUnits() {
        Entity ce = ce();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<Integer, Vector<Integer>>();

        Vector<Entity> launchableFighters = ce.getLaunchableFighters();
        Vector<Entity> launchableSmallCraft = ce.getLaunchableSmallCraft();

        // Handle error condition.
        if ((launchableFighters.size() <= 0)
                && (launchableSmallCraft.size() <= 0)) {
            System.err
                    .println("MovementDisplay#getUnloadedUnit() called without loaded units."); //$NON-NLS-1$

        } else {
            // cycle through the fighter bays and then the small craft bays
            int bayNum = 1;
            Bay currentBay;
            Vector<Entity> currentFighters = new Vector<Entity>();
            int doors = 0;
            Vector<Bay> FighterBays = ce.getFighterBays();
            for (int i = 0; i < FighterBays.size(); i++) {
                currentBay = FighterBays.elementAt(i);
                Vector<Integer> bayChoices = new Vector<Integer>();
                currentFighters = currentBay.getLaunchableUnits();
                /*
                 * We will assume that if more fighters are launched than is
                 * safe, that these excess fighters will be distributed equally
                 * among available doors
                 */
                doors = currentBay.getDoors();
                if (currentFighters.size() > 0) {
                    String[] names = new String[currentFighters.size()];
                    String question = Messages
                            .getString(
                                    "MovementDisplay.LaunchFighterDialog.message", new Object[] { //$NON-NLS-1$
                                    ce.getShortName(), doors * 2, bayNum });
                    for (int loop = 0; loop < names.length; loop++) {
                        names[loop] = currentFighters.elementAt(loop)
                                .getShortName();
                    }
                    
                    boolean doIt = false;
                    ChoiceDialog choiceDialog = new ChoiceDialog(
                            clientgui.frame,
                            Messages
                                    .getString(
                                            "MovementDisplay.LaunchFighterDialog.title", new Object[] { //$NON-NLS-1$
                                            currentBay.getType(), bayNum }),
                            question, names);;
                    while(!doIt) {
                        choiceDialog = new ChoiceDialog(
                                clientgui.frame,
                                Messages
                                        .getString(
                                                "MovementDisplay.LaunchFighterDialog.title", new Object[] { //$NON-NLS-1$
                                                currentBay.getType(), bayNum }),
                                question, names);
                        choiceDialog.setVisible(true);
                        if(choiceDialog.getChoices().length > (doors * 2)) {
                            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages
                                    .getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                                    Messages.getString("MovementDisplay.ConfirmLaunch"), true);
                            nag.setVisible(true);
                            doIt = nag.getAnswer();
                        } else {
                            doIt = true;
                        }
                    }
                    if (choiceDialog.getAnswer() == true && doIt) {
                        // load up the choices
                        int[] unitsLaunched = choiceDialog.getChoices();
                        for (int element : unitsLaunched) {
                            bayChoices.add(currentFighters.elementAt(element)
                                    .getId());
                        }
                        choices.put(i, bayChoices);
                        // now remove them (must be a better way?)
                        for (int l = unitsLaunched.length; l > 0; l--) {
                            currentFighters.remove(unitsLaunched[l - 1]);
                        }
                    }
                }
                bayNum++;
            }
        }// End have-choices
        // Return the chosen unit.
        return choices;
    }

    /**
     * Get the unit that the player wants to drop. This method will remove the
     * unit from our local copy of loaded units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This
     *         value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getDroppedUnits() {
        Entity ce = ce();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<Integer, Vector<Integer>>();

        Vector<Entity> droppableUnits = ce.getDroppableUnits();

        // Handle error condition.
        if (droppableUnits.size() <= 0) {
            System.err
                    .println("MovementDisplay#getDroppedUnits() called without loaded units."); //$NON-NLS-1$

        } else {
            // cycle through the bays
            int bayNum = 1;
            Bay currentBay;
            Vector<Entity> currentUnits = new Vector<Entity>();
            int doors = 0;
            Vector<Bay> Bays = ce.getTransportBays();
            for (int i = 0; i < Bays.size(); i++) {
                currentBay = Bays.elementAt(i);
                Vector<Integer> bayChoices = new Vector<Integer>();
                currentUnits = currentBay.getDroppableUnits();
                doors = currentBay.getDoors();
                if ((currentUnits.size() > 0) && (doors > 0)) {
                    String[] names = new String[currentUnits.size()];
                    String question = Messages
                            .getString(
                                    "MovementDisplay.DropUnitDialog.message", new Object[] { //$NON-NLS-1$
                                     doors, bayNum });
                    for (int loop = 0; loop < names.length; loop++) {
                        names[loop] = currentUnits.elementAt(loop)
                                .getShortName();
                    }
                    ChoiceDialog choiceDialog = new ChoiceDialog(
                            clientgui.frame,
                            Messages
                                    .getString(
                                            "MovementDisplay.DropUnitDialog.title", new Object[] { //$NON-NLS-1$
                                            currentBay.getType(), bayNum }),
                            question, names, false, doors);
                    choiceDialog.setVisible(true);
                    if (choiceDialog.getAnswer() == true) {
                        // load up the choices
                        int[] unitsLaunched = choiceDialog.getChoices();
                        for (int element : unitsLaunched) {
                            bayChoices.add(currentUnits.elementAt(element)
                                    .getId());
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
        }// End have-choices
        // Return the chosen unit.
        return choices;
    }

    /**
     * get the unit id that the player wants to be recovered by
     */
    private int getRecoveryUnit() {
        Entity ce = ce();
        List<Entity> choices = new ArrayList<Entity>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (clientgui.getClient().game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd
                    .getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = clientgui.getClient().game
                .getEntities(loadeePos);
        while (entities.hasMoreElements()) {
            other = entities.nextElement();
            // Is the other unit friendly and not the current entity?
                // must be done with its movement
                // it also must be same heading and velocity
                if ((other instanceof Aero)
                        && !((Aero) other).isOutControlTotal()
                        && other.isDone() && other.canLoad(ce)
                        && ce.isLoadableThisTurn()
                        && (cmd.getFinalFacing() == other.getFacing())) {

                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (clientgui.getClient().game.useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa
                                .getVectors())) {
                            choices.add(other);
                        }
                    } else if (cmd.getFinalVelocity() == oa
                            .getCurrentVelocity()) {
                        choices.add(other);
                    }
                }
            // Nope. Discard it.
            other = null;
        } // Check the next entity in this position.

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            if (choices.get(0).mpUsed > 0) {
                if (clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("MovementDisplay.RecoverSureDialog.title"), //$NON-NLS-1$
                                Messages
                                        .getString("MovementDisplay.RecoverSureDialog.message") //$NON-NLS-1$
                        )) {
                    return choices.get(0).getId();
                }
            } else {
                return choices.get(0).getId();
            }
            return -1;
        }

        String input = (String) JOptionPane
                .showInputDialog(
                        clientgui,
                        Messages
                                .getString("MovementDisplay.RecoverFighterDialog.message"), //$NON-NLS-1$
                        Messages
                                .getString("MovementDisplay.RecoverFighterDialog.title"), //$NON-NLS-1$
                        JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                .getDisplayArray(choices), null);
        Entity picked = (Entity) SharedUtility.getTargetPicked(choices, input);

        if (picked != null) {
            // if this unit is thrusting, make sure they are aware
            if (picked.mpUsed > 0) {
                if (clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("MovementDisplay.RecoverSureDialog.title"), //$NON-NLS-1$
                                Messages
                                        .getString("MovementDisplay.RecoverSureDialog.message") //$NON-NLS-1$
                        )) {
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
        Entity ce = ce();
        List<Entity> choices = new ArrayList<Entity>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (clientgui.getClient().game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd
                    .getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = clientgui.getClient().game
                .getEntities(loadeePos);
        while (entities.hasMoreElements()) {
            other = entities.nextElement();
            // Is the other unit friendly and not the current entity?
                // must be done with its movement
                // it also must be same heading and velocity
                if ((other instanceof Aero)
                        && !((Aero) other).isOutControlTotal()
                        && other.isDone() && other.canLoad(ce)
                        && ce.isLoadableThisTurn()
                        && (cmd.getFinalFacing() == other.getFacing())) {

                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (clientgui.getClient().game.useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa
                                .getVectors())) {
                            choices.add(other);
                        }
                    } else if (cmd.getFinalVelocity() == oa
                            .getCurrentVelocity()) {
                        choices.add(other);
                    }
                }
            // Nope. Discard it.
            other = null;
        } // Check the next entity in this position.

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            return choices.get(0).getId();
        }

        String input = (String) JOptionPane.showInputDialog(clientgui, Messages
                .getString("MovementDisplay.JoinSquadronDialog.message"), //$NON-NLS-1$
                Messages.getString("MovementDisplay.JoinSquadronDialog.title"), //$NON-NLS-1$
                JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                        .getDisplayArray(choices), null);
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

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        if (a.isOutControlTotal() && a.isAirborne()) {
            disableButtons();
            butDone.setEnabled(true);
            butNext.setEnabled(true);
            setForwardIniEnabled(true);
            butLaunch.setEnabled(true);
        }
        return;
    }

    /**
     * check for fuel and adjust buttons
     */
    private void checkFuel() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;
        if (a.getFuel() < 1) {
            disableButtons();
            butDone.setEnabled(true);
            butNext.setEnabled(true);
            setForwardIniEnabled(true);
            butLaunch.setEnabled(true);
            updateRACButton();
            updateJoinButton();
            updateRecoveryButton();
            updateDumpButton();
        }
        return;
    }

    /**
     * check for atmosphere and adjust buttons
     */
    private void checkAtmosphere() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;
        if (!clientgui.getClient().game.getBoard().inSpace()) {
            if (a.isSpheroid()
                    || clientgui.getClient().game.getPlanetaryConditions()
                            .isVacuum()) {
                butAcc.setEnabled(false);
                butDec.setEnabled(false);
                butAccN.setEnabled(false);
                butDecN.setEnabled(false);
            }
        }
        return;
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Entity ce = ce();

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = clientgui.getClient().game
                .getEntities(pos);

        // Convert the choices into a List of targets.
        ArrayList<Targetable> targets = new ArrayList<Targetable>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!ce.equals(choice)) {
                targets.add(choice);
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().game.getBoard()
                .getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().game
                    .getBoard(), false));
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages
                                    .getString(
                                            "MovementDisplay.ChooseTargetDialog.message", new Object[] {//$NON-NLS-1$
                                            pos.getBoardNum() }),
                            Messages
                                    .getString("MovementDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;
    } // End private Targetable chooseTarget( Coords )

    private int chooseMineToLay() {
        MineLayingDialog mld = new MineLayingDialog(clientgui.frame, ce());
        mld.setVisible(true);
        if (mld.getAnswer()) {
            return mld.getMine();
        }
        return -1;
    }

    private void dumpBombs() {

        if (!(ce() instanceof Aero)) {
            return;
        }
        Aero a = (Aero) ce();

        EntityMovementType overallMoveType = EntityMovementType.MOVE_NONE;
        if (null != cmd) {
            overallMoveType = cmd.getLastStepMovementType();
        }
        // bring up dialog to dump bombs, then make a control roll and report
        // success or failure
        // should update mp available
        BombPayloadDialog dumpBombsDialog = new BombPayloadDialog(
                clientgui.frame, Messages
                        .getString("MovementDisplay.BombDumpDialog.title"), //$NON-NLS-1$
                a.getBombChoices(), false, true, -1);
        dumpBombsDialog.setVisible(true);
        if (dumpBombsDialog.getAnswer()) {
            int[] bombsDumped = dumpBombsDialog.getChoices();
            // first make a control roll
            PilotingRollData psr = ce().getBasePilotingRoll(overallMoveType);
            int ctrlroll = Compute.d6(2);
            Report r = new Report(9500);
            r.subject = ce().getId();
            r.add(ce().getDisplayName());
            r.add(psr.getValue());
            r.add(ctrlroll);
            r.newlines = 0;
            r.indent(1);
            if (ctrlroll < psr.getValue()) {
                r.choose(false);
                // addReport(r);
                String title = Messages
                        .getString("MovementDisplay.DumpingBombs.title"); //$NON-NLS-1$
                String body = Messages
                        .getString("MovementDisplay.DumpFailure.message"); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
                // failed the roll, so dump all bombs
                bombsDumped = a.getBombChoices();
            } else {
                // avoided damage
                r.choose(true);
                String title = Messages
                        .getString("MovementDisplay.DumpingBombs.title"); //$NON-NLS-1$
                String body = Messages
                        .getString("MovementDisplay.DumpSuccessful.message"); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
                // addReport(r);
            }
            /*
             * // now unload the bombs for (int j = 0; j < bombsDumped.length;
             * j++) { if (bombsDumped[j] > 0) { a.removeBombs(bombsDumped[j],
             * j); // Report r = new Report(5115); r.subject = ce().getId();
             * r.addDesc(ce()); r.add("" + bombsDumped[j] + " " +
             * Aero.bombNames[j] + "(s)"); // addReport(r); } } // update the
             * bomb load immediately a.updateBombLoad();
             * clientgui.getClient().sendUpdateEntity(ce());
             */
            // not sure how to update mech display, but everything else is
            // working
        }

    }

    /**
     * based on maneuver type add the appropriate steps return true if we should
     * redraw the movement data
     */
    private boolean addManeuver(int type) {
        cmd.addManeuver(type);
        switch (type) {
        case (ManeuverType.MAN_HAMMERHEAD):
            cmd.addStep(MoveStepType.YAW, true, true);
            return true;
        case (ManeuverType.MAN_HALF_ROLL):
            cmd.addStep(MoveStepType.ROLL, true, true);
            return true;
        case (ManeuverType.MAN_BARREL_ROLL):
            cmd.addStep(MoveStepType.DEC, true, true);
            return true;
        case (ManeuverType.MAN_IMMELMAN):
            gear = MovementDisplay.GEAR_IMMEL;
            return false;
        case (ManeuverType.MAN_SPLIT_S):
            gear = MovementDisplay.GEAR_SPLIT_S;
            return false;
        case (ManeuverType.MAN_VIFF):
            if (!(ce() instanceof Aero)) {
                return false;
            }
            Aero a = (Aero) ce();
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
        case (ManeuverType.MAN_SIDE_SLIP_LEFT):
            cmd.addStep(MoveStepType.LATERAL_LEFT, true, true);
            return true;
        case (ManeuverType.MAN_SIDE_SLIP_RIGHT):
            cmd.addStep(MoveStepType.LATERAL_RIGHT, true, true);
            return true;
        case (ManeuverType.MAN_LOOP):
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
        if (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_MOVEMENT) {
            // ignore
            return;
        }
        // else, change turn
        endMyTurn();
        if (clientgui.getClient().isMyTurn()) {
            // Can the player unload entities stranded on immobile transports?
            if (clientgui.getClient().canUnloadStranded()) {
                unloadStranded();
            } else {
                beginMyTurn();
            }
        } else {
            if ((e.getPlayer() == null)
                    && (clientgui.getClient().game.getTurn() instanceof GameTurn.UnloadStrandedTurn)) {
                setStatusBarText(Messages
                        .getString("MovementDisplay.waitForAnother")); //$NON-NLS-1$
            } else {
                setStatusBarText(Messages
                        .getString(
                                "MovementDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
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
                && (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
            endMyTurn();
        }
        if (clientgui.getClient().game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
            setStatusBarText(Messages
                    .getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$
        }
    }

    //
    // ActionListener
    //
    public synchronized void actionPerformed(ActionEvent ev) {
        final Entity ce = ce();
        
        if (ce == null){
            return;
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (statusBarActionPerformed(ev, clientgui.getClient())) {
            return;
        }
        if (!clientgui.getClient().isMyTurn()) {
            // odd...
            return;
        }
        if (ev.getActionCommand().equals(MOVE_NEXT)) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(MOVE_FORWARD_INI)) {
            selectNextPlayer();
        } else if (ev.getActionCommand().equals(MOVE_CANCEL)) {
            clear();
        } else if (ev.getSource().equals(butMore)) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(MOVE_UNJAM)) {
            if ((gear == MovementDisplay.GEAR_JUMP)
                    || (gear == MovementDisplay.GEAR_CHARGE)
                    || (gear == MovementDisplay.GEAR_DFA)
                    || ((cmd.getMpUsed() > ce.getWalkMP()) && !(cmd.getLastStep().isOnlyPavement() && (cmd.getMpUsed() <= (ce.getWalkMP() + 1))))
                    || (gear == MovementDisplay.GEAR_SWIM)
                    || (gear == MovementDisplay.GEAR_RAM)) {
                // in the wrong gear
                // clearAllMoves();
                // gear = Compute.GEAR_LAND;
                setUnjamEnabled(false);
            } else {
                cmd.addStep(MoveStepType.UNJAM_RAC);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_SEARCHLIGHT)) {
            cmd.addStep(MoveStepType.SEARCHLIGHT);
        } else if (ev.getActionCommand().equals(MOVE_WALK)) {
            if ((gear == MovementDisplay.GEAR_JUMP)
                    || (gear == MovementDisplay.GEAR_SWIM)) {
                clear();
            }
            gear = MovementDisplay.GEAR_LAND;
        } else if (ev.getActionCommand().equals(MOVE_JUMP)) {
            if ((gear != MovementDisplay.GEAR_JUMP) &&
                    !((cmd.getLastStep() != null) && cmd.getLastStep().isFirstStep() && (cmd.getLastStep().getType() == MoveStepType.LAY_MINE))) {
                clear();
            }
            if (!cmd.isJumping()) {
                cmd.addStep(MoveStepType.START_JUMP);
            }
            gear = MovementDisplay.GEAR_JUMP;
        } else if (ev.getActionCommand().equals(MOVE_SWIM)) {
            if (gear != MovementDisplay.GEAR_SWIM) {
                clear();
            }
            // dcmd.addStep(MoveStepType.SWIM);
            gear = MovementDisplay.GEAR_SWIM;
            ce
                    .setMovementMode((ce instanceof BipedMech) ? EntityMovementMode.BIPED_SWIM
                            : EntityMovementMode.QUAD_SWIM);
        } else if (ev.getActionCommand().equals(MOVE_TURN)) {
            gear = MovementDisplay.GEAR_TURN;
        } else if (ev.getActionCommand().equals(MOVE_BACK_UP)) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_BACKUP;
        } else if (ev.getActionCommand().equals(MOVE_CLEAR)) {
            clear();
            if (!clientgui.getClient().game.containsMinefield(ce.getPosition())) {
                clientgui.doAlertDialog(Messages
                        .getString("MovementDisplay.CantClearMinefield"), //$NON-NLS-1$
                        Messages.getString("MovementDisplay.NoMinefield")); //$NON-NLS-1$
                return;
            }

            // Does the entity has a minesweeper?
            int clear = Minefield.CLEAR_NUMBER_INFANTRY;
            int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;
            /*
             * for (Mounted mounted : ce.getMisc()) { if
             * (mounted.getType().hasFlag(MiscType.F_TOOLS) &&
             * mounted.getType().hasSubType(MiscType.S_MINESWEEPER)) { int
             * sweeperType = mounted.getType().getToHitModifier(); clear =
             * Minefield.CLEAR_NUMBER_SWEEPER[sweeperType]; boom =
             * Minefield.CLEAR_NUMBER_SWEEPER_ACCIDENT[sweeperType]; break; } }
             */

            // need to choose a mine
            List<Minefield> mfs = clientgui.getClient().game.getMinefields(ce
                    .getPosition());
            String[] choices = new String[mfs.size()];
            for (int loop = 0; loop < choices.length; loop++) {
                choices[loop] = Minefield.getDisplayableName(mfs.get(loop)
                        .getType());
            }
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages
                                    .getString("MovementDisplay.ChooseMinefieldDialog.message"),//$NON-NLS-1$
                            Messages
                                    .getString("MovementDisplay.ChooseMinefieldDialog.title"), //$NON-NLS-1$
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

            if ((null != mf)
                    && clientgui
                            .doYesNoDialog(
                                    Messages
                                            .getString("MovementDisplay.ClearMinefieldDialog.title"), //$NON-NLS-1$
                                    Messages
                                            .getString(
                                                    "MovementDisplay.ClearMinefieldDialog.message", new Object[] {//$NON-NLS-1$
                                                    new Integer(clear),
                                                            new Integer(boom) }))) {
                cmd.addStep(MoveStepType.CLEAR_MINEFIELD, mf);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_CHARGE)) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            gear = MovementDisplay.GEAR_CHARGE;
        } else if (ev.getActionCommand().equals(MOVE_DFA)) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_DFA;
            if (!cmd.isJumping()) {
                cmd.addStep(MoveStepType.START_JUMP);
            }
        } else if (ev.getActionCommand().equals(MOVE_RAM)) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            gear = MovementDisplay.GEAR_RAM;
        } else if (ev.getActionCommand().equals(MOVE_GET_UP)) {
            // if the unit has a hull down step
            // then don't clear the moves
            if (!cmd.contains(MoveStepType.HULL_DOWN)) {
                clear();
            }

            if (clientgui.getClient().game.getOptions().booleanOption(
                    "tacops_careful_stand")
                    && (ce.getWalkMP() > 2)) {
                ConfirmDialog response = clientgui
                        .doYesNoBotherDialog(
                                Messages
                                        .getString("MovementDisplay.CarefulStand.title"),//$NON-NLS-1$
                                Messages
                                        .getString("MovementDisplay.CarefulStand.message"));
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
                butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Move")+"</b></html"); //$NON-NLS-1$
                if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                    cmd.addStep(MoveStepType.GET_UP);
                }
            }

            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_GO_PRONE)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                cmd.addStep(MoveStepType.GO_PRONE);
            }
            clientgui.bv.drawMovementData(ce(), cmd);
            butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Move")+"</b></html>"); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_HULL_DOWN)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalHullDown()) {
                cmd.addStep(MoveStepType.HULL_DOWN);
            }
            clientgui.bv.drawMovementData(ce(), cmd);
            butDone.setText("<html><b>"+Messages.getString("MovementDisplay.Move")+"</b></html>"); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_FLEE)
                && clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("MovementDisplay.EscapeDialog.title"), Messages.getString("MovementDisplay.EscapeDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
            clear();
            cmd.addStep(MoveStepType.FLEE);
            ready();
        } else if (ev.getActionCommand().equals(MOVE_FLY_OFF)
                && clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("MovementDisplay.FlyOffDialog.title"), Messages.getString("MovementDisplay.FlyOffDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
            // clear();
            if(clientgui.getClient().game.getOptions().booleanOption("return_flyover")
                    && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ReturnFly.title"), Messages.getString("MovementDisplay.ReturnFly.message"))) {
                cmd.addStep(MoveStepType.RETURN);
            } else {
                cmd.addStep(MoveStepType.OFF);
            }
            ready();
        } else if (ev.getActionCommand().equals(MOVE_EJECT)) {
            if (ce instanceof Tank) {
                if (clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("MovementDisplay.AbandonDialog.title"), Messages.getString("MovementDisplay.AbandonDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    cmd.addStep(MoveStepType.EJECT);
                    ready();
                }
            } else if (clientgui
                    .doYesNoDialog(
                            Messages
                                    .getString("MovementDisplay.AbandonDialog1.title"), Messages.getString("MovementDisplay.AbandonDialog1.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                clear();
                cmd.addStep(MoveStepType.EJECT);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_LOAD)) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = null;
            Enumeration<Entity> entities = clientgui.getClient().game
                    .getEntities(ce.getPosition());
            while (entities.hasMoreElements()) {
                other = entities.nextElement();
                //if (ce.getOwner().equals(other.getOwner()) && !ce.equals(other)) {
                if ((ce != null) && !ce.equals(other) && ce.canLoad(other, false)) {
                    loadedUnits.add(other);
                    break;
                }
                other = null;
            }
            if (other != null) {
                cmd.addStep(MoveStepType.LOAD);
                clientgui.bv.drawMovementData(ce(), cmd);
                gear = MovementDisplay.GEAR_LAND;
            } // else - didn't find a unit to load
        } else if(ev.getActionCommand().equals(MOVE_MOUNT)) {
            Entity other = getMountedUnit();
            if(other != null) {
                cmd.addStep(MoveStepType.MOUNT, other);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_UNLOAD)) {
            // Ask the user if we're carrying multiple units.
            Entity other = getUnloadedUnit();
            if (other != null) {
                if(ce() instanceof SmallCraft) {
                    Coords pos = getUnloadPosition(other);
                    if(null != pos) {
                        //set other's position and end this turn - the unloading unit will get
                        //another turn for further unloading later
                        cmd.addStep(MoveStepType.UNLOAD, other, pos);
                        clientgui.bv.drawMovementData(ce(), cmd);
                        ready();
                    }
                } else {
                    //some different handling for small craft/dropship unloading
                    cmd.addStep(MoveStepType.UNLOAD, other);
                    clientgui.bv.drawMovementData(ce(), cmd);
                }
            } // else - Player canceled the unload.
        } else if (ev.getActionCommand().equals(MOVE_RAISE_ELEVATION)) {
            cmd.addStep(MoveStepType.UP);
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_LOWER_ELEVATION)) {
            if ((ce instanceof Aero)
                    && (null != cmd.getLastStep())
                    && (cmd.getLastStep().getNDown() == 1)
                    && (cmd.getLastStep().getVelocity() < 12)
                    && !(((Aero) ce).isSpheroid() || clientgui.getClient().game
                            .getPlanetaryConditions().isVacuum())) {
                cmd.addStep(MoveStepType.ACC, true);
            }
            cmd.addStep(MoveStepType.DOWN);
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_CLIMB_MODE)) {
            MoveStep ms = cmd.getLastStep();
            if ((ms != null)
                    && ((ms.getType() == MoveStepType.CLIMB_MODE_ON) || (ms
                            .getType() == MoveStepType.CLIMB_MODE_OFF))) {
                cmd.removeLastStep();
            } else if (cmd.getFinalClimbMode()) {
                cmd.addStep(MoveStepType.CLIMB_MODE_OFF);
            } else {
                cmd.addStep(MoveStepType.CLIMB_MODE_ON);
            }
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_LAY_MINE)) {
            int i = chooseMineToLay();
            if (i != -1) {
                Mounted m = ce().getEquipment(i);
                if (m.getMineType() == Mounted.MINE_VIBRABOMB) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                            clientgui.frame);
                    vsd.setVisible(true);
                    m.setVibraSetting(vsd.getSetting());
                }
                cmd.addStep(MoveStepType.LAY_MINE, i);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_DIG_IN)) {
            cmd.addStep(MoveStepType.DIG_IN);
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_FORTIFY)) {
            cmd.addStep(MoveStepType.FORTIFY);
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_SHAKE_OFF)) {
            cmd.addStep(MoveStepType.SHAKE_OFF_SWARMERS);
            clientgui.bv.drawMovementData(ce(), cmd);
        } else if (ev.getActionCommand().equals(MOVE_RECKLESS)) {
            cmd.setCareful(false);
        } else if (ev.getActionCommand().equals(MOVE_ACCN)) {
            cmd.addStep(MoveStepType.ACCN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DECN)) {
            cmd.addStep(MoveStepType.DECN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_ACC)) {
            cmd.addStep(MoveStepType.ACC);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DEC)) {
            cmd.addStep(MoveStepType.DEC);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_EVADE)) {
            cmd.addStep(MoveStepType.EVADE);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_SHUTDOWN)) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ShutdownDialog.title"),
                                        Messages.getString("MovementDisplay.ShutdownDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                clear();
                cmd.addStep(MoveStepType.SHUTDOWN);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_STARTUP)) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.StartupDialog.title"),
                                        Messages.getString("MovementDisplay.StartupDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                clear();
                cmd.addStep(MoveStepType.STARTUP);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_SELF_DESTRUCT)) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.SelfDestructDialog.title"),
                                        Messages.getString("MovementDisplay.SelfDestructDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                cmd.addStep(MoveStepType.SELF_DESTRUCT);
                ready();
            }
        } else if (ev.getActionCommand().equals(MOVE_EVADE_AERO)) {
            cmd.addStep(MoveStepType.EVADE);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_ROLL)) {
            cmd.addStep(MoveStepType.ROLL);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_HOVER)) {
            cmd.addStep(MoveStepType.HOVER);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_MANEUVER)) {
            ManeuverChoiceDialog choiceDialog = new ManeuverChoiceDialog(
                    clientgui.frame, Messages
                            .getString("MovementDisplay.ManeuverDialog.title"), //$NON-NLS-1$
                    "huh?");
            Aero a = (Aero) ce;
            MoveStep last = cmd.getLastStep();
            int vel = a.getCurrentVelocity();
            int altitude = a.getAltitude();
            Coords pos = a.getPosition();
            int distance = 0;
            if (null != last) {
                vel = last.getVelocityLeft();
                altitude = last.getAltitude();
                pos = last.getPosition();
                distance = last.getDistance();
            }
            int ceil = clientgui.getClient().game.getBoard().getHex(pos)
                    .ceiling();
            choiceDialog.checkPerformability(vel, altitude, ceil, a.isVSTOL(),
                    distance);
            choiceDialog.setVisible(true);
            int manType = choiceDialog.getChoice();
            if ((manType > ManeuverType.MAN_NONE) && addManeuver(manType)) {
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_LAUNCH)) {
            TreeMap<Integer, Vector<Integer>> launched = getLaunchedUnits();
            if (!launched.isEmpty()) {
                cmd.addStep(MoveStepType.LAUNCH, launched);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_RECOVER)) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int recoverer = getRecoveryUnit();
            if (recoverer != -1) {
                cmd.addStep(MoveStepType.RECOVER, recoverer, -1);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_DROP)) {
            TreeMap<Integer, Vector<Integer>> dropped = getDroppedUnits();
            if (!dropped.isEmpty()) {
                cmd.addStep(MoveStepType.DROP, dropped);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_JOIN)) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int joined = getUnitJoined();
            if (joined != -1) {
                cmd.addStep(MoveStepType.JOIN, joined, -1);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_TURN_LEFT)) {
            cmd.addStep(MoveStepType.TURN_LEFT);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_TURN_RIGHT)) {
            cmd.addStep(MoveStepType.TURN_RIGHT);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_THRUST)) {
            cmd.addStep(MoveStepType.THRUST);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_YAW)) {
            cmd.addStep(MoveStepType.YAW);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_END_OVER)) {
            cmd.addStep(MoveStepType.YAW);
            cmd.addStep(MoveStepType.ROLL);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DUMP)) {
            dumpBombs();
        }
        else if (ev.getActionCommand().equals(MOVE_TAKE_OFF)) {
            if((ce() instanceof Aero) && (null != ((Aero)ce()).hasRoomForHorizontalTakeOff())) {
                String title = Messages.getString("MovementDisplay.NoTakeOffDialog.title"); //$NON-NLS-1$
                String body = Messages.getString("MovementDisplay.NoTakeOffDialog.message", new Object[] {((Aero)ce()).hasRoomForHorizontalTakeOff()}); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
            } else {
                if(clientgui.doYesNoDialog(Messages.getString("MovementDisplay.TakeOffDialog.title"), Messages.getString("MovementDisplay.TakeOffDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    cmd.addStep(MoveStepType.TAKEOFF);
                    ready();
                }
            }
        }
        else if (ev.getActionCommand().equals(MOVE_VERT_TAKE_OFF)) {
            if(clientgui.doYesNoDialog(Messages.getString("MovementDisplay.TakeOffDialog.title"), Messages.getString("MovementDisplay.TakeOffDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                clear();
                cmd.addStep(MoveStepType.VTAKEOFF);
                ready();
            }
        }
        else if (ev.getActionCommand().equals(MOVE_LAND)) {
            if((ce() instanceof Aero) && (null != ((Aero)ce()).hasRoomForHorizontalLanding())) {
                String title = Messages.getString("MovementDisplay.NoLandingDialog.title"); //$NON-NLS-1$
                String body = Messages.getString("MovementDisplay.NoLandingDialog.message", new Object[] {((Aero)ce()).hasRoomForHorizontalLanding()}); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
            } else {
                if(clientgui.doYesNoDialog(Messages.getString("MovementDisplay.LandDialog.title"), Messages.getString("MovementDisplay.LandDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    cmd.addStep(MoveStepType.LAND);
                    ready();
                }
            }
        }
        else if (ev.getActionCommand().equals(MOVE_VERT_LAND)) {
            if((ce() instanceof Aero) && (null != ((Aero)ce()).hasRoomForVerticalLanding())) {
                String title = Messages.getString("MovementDisplay.NoLandingDialog.title"); //$NON-NLS-1$
                String body = Messages.getString("MovementDisplay.NoLandingDialog.message", new Object[] {((Aero)ce()).hasRoomForVerticalLanding()}); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
            } else {
                if(clientgui.doYesNoDialog(Messages.getString("MovementDisplay.LandDialog.title"), Messages.getString("MovementDisplay.LandDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clear();
                    cmd.addStep(MoveStepType.VLAND);
                    ready();
                }
            }
        }
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateLoadButtons();
        updateElevationButtons();
        updateTakeOffButtons();
        updateLandButtons();
        updateFlyOffButton();
        updateLaunchButton();
        updateLoadButtons();
        updateDropButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();
        updateDumpButton();
        updateEvadeButton();
        updateShutdownButton();
        updateStartupButton();
        updateSelfDestructButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();

        //if small craft/dropship that has unloaded units, then only allowed
        //to unload more
        if(ce.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }

    }

    /**
     * Give the player the opportunity to unload all entities that are stranded
     * on immobile transports.
     * <p/>
     * According to <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart="
     * > Randall Bills</a>, the "minimum move" rule allow stranded units to
     * dismount at the start of the turn.
     */
    private void unloadStranded() {
        Vector<Entity> stranded = new Vector<Entity>();
        String[] names = null;
        Entity entity = null;
        Entity transport = null;

        // Let the player know what's going on.
        setStatusBarText(Messages.getString("MovementDisplay.AllPlayersUnload")); //$NON-NLS-1$

        // Collect the stranded entities into the vector.
        // TODO : get a better interface to "game" and "turn"
        Enumeration<Entity> entities = clientgui.getClient()
                .getSelectedEntities(new EntitySelector() {
                    private final IGame game = clientgui.getClient().game;
                    private final GameTurn turn = clientgui.getClient().game
                            .getTurn();
                    private final int ownerId = clientgui.getClient()
                            .getLocalPlayer().getId();

                    public boolean accept(Entity acc) {
                        if (turn.isValid(ownerId, acc, game)) {
                            return true;
                        }
                        return false;
                    }
                });
        while (entities.hasMoreElements()) {
            stranded.addElement(entities.nextElement());
        }

        // Construct an array of stranded entity names
        names = new String[stranded.size()];
        for (int index = 0; index < names.length; index++) {
            entity = stranded.elementAt(index);
            transport = clientgui.getClient()
                    .getEntity(entity.getTransportId());
            String buffer;
            if (null == transport) {
                buffer = entity.getDisplayName();
            } else {
                buffer = Messages
                        .getString(
                                "MovementDisplay.EntityAt", new Object[] { entity.getDisplayName(), transport.getPosition().getBoardNum() }); //$NON-NLS-1$
            }
            names[index] = buffer.toString();
        }

        // Show the choices to the player

        int[] indexes = clientgui
                .doChoiceDialog(
                        Messages
                                .getString("MovementDisplay.UnloadStrandedUnitsDialog.title"), //$NON-NLS-1$
                        Messages
                                .getString("MovementDisplay.UnloadStrandedUnitsDialog.message"), //$NON-NLS-1$
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

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (clientgui.getClient().isMyTurn()) {
                removeLastStep();
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && !shiftheld) {
            shiftheld = true;
            if (clientgui.getClient().isMyTurn()
                    && (clientgui.getBoardView().getLastCursor() != null)
                    && !clientgui.getBoardView().getLastCursor().equals(
                            clientgui.getBoardView().getSelected())) {
                // switch to turning
                // clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
                clientgui.bv.drawMovementData(ce(), cmd);
            }
        }

        // arrow can also rotate when shift is down
        if (shiftheld
                && clientgui.getClient().isMyTurn()
                && ((ev.getKeyCode() == KeyEvent.VK_LEFT) || (ev.getKeyCode() == KeyEvent.VK_RIGHT))) {
            int curDir = cmd.getFinalFacing();
            int dir = curDir;
            if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
                dir = (dir + 5) % 6;
            } else {
                dir = (dir + 7) % 6;
            }
            Coords curPos = cmd.getFinalCoords();
            Coords target = curPos.translated(dir);
            currentMove(target);
            clientgui.bv.drawMovementData(ce(), cmd);
        }
    }

    public void keyReleased(KeyEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && shiftheld) {
            shiftheld = false;
            if (clientgui.getClient().isMyTurn()
                    && (clientgui.getBoardView().getLastCursor() != null)
                    && !clientgui.getBoardView().getLastCursor().equals(
                            clientgui.getBoardView().getSelected())) {
                // switch to movement
                clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
                clientgui.bv.drawMovementData(ce(), cmd);
            }
        }
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
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
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce.getPosition());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        Entity e = clientgui.getClient().game.getEntity(b.getEntityId());
        if (null == e) {
            return;
        }
        if (clientgui.getClient().isMyTurn()) {
            if (clientgui.getClient().game.getTurn().isValidEntity(e,
                    clientgui.getClient().game)) {
                selectEntity(e.getId());
            }
        } else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
                clientgui.bv.centerOnHex(e.getPosition());
            }
        }
    }

    private void setWalkEnabled(boolean enabled) {
        butWalk.setEnabled(enabled);
        clientgui.getMenuBar().setMoveWalkEnabled(enabled);
    }

    private void setTurnEnabled(boolean enabled) {
        butTurn.setEnabled(enabled);
        clientgui.getMenuBar().setMoveTurnEnabled(enabled);
    }

    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setMoveNextEnabled(enabled);
    }

    private void setForwardIniEnabled(boolean enabled) {
        //forward initiative can only be done if Teams have an initiative!
        if (clientgui.getClient().game.getOptions().booleanOption("team_initiative")) {
            butForwardIni.setEnabled(enabled);
            clientgui.getMenuBar().setMoveForwardIniEnabled(enabled);
        } else { //turn them off regardless what is said!
            butForwardIni.setEnabled(false);
            clientgui.getMenuBar().setMoveForwardIniEnabled(false);
        }
    }

    private void setLayMineEnabled(boolean enabled) {
        butLayMine.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLayMineEnabled(enabled);
    }

    private void setLoadEnabled(boolean enabled) {
        butLoad.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLoadEnabled(enabled);
    }

    private void setMountEnabled(boolean enabled) {
        butMount.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveMountEnabled(enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
        butUnload.setEnabled(enabled);
        clientgui.getMenuBar().setMoveUnloadEnabled(enabled);
    }

    private void setJumpEnabled(boolean enabled) {
        butJump.setEnabled(enabled);
        clientgui.getMenuBar().setMoveJumpEnabled(enabled);
    }

    private void setSwimEnabled(boolean enabled) {
        butSwim.setEnabled(enabled);
        clientgui.getMenuBar().setMoveSwimEnabled(enabled);
    }

    private void setBackUpEnabled(boolean enabled) {
        butBackup.setEnabled(enabled);
        clientgui.getMenuBar().setMoveBackUpEnabled(enabled);
    }

    private void setChargeEnabled(boolean enabled) {
        butCharge.setEnabled(enabled);
        clientgui.getMenuBar().setMoveChargeEnabled(enabled);
    }

    private void setDFAEnabled(boolean enabled) {
        butDfa.setEnabled(enabled);
        clientgui.getMenuBar().setMoveDFAEnabled(enabled);
    }

    private void setGoProneEnabled(boolean enabled) {
        butDown.setEnabled(enabled);
        clientgui.getMenuBar().setMoveGoProneEnabled(enabled);
    }

    private void setFleeEnabled(boolean enabled) {
        butFlee.setEnabled(enabled);
        clientgui.getMenuBar().setMoveFleeEnabled(enabled);
    }

    private void setFlyOffEnabled(boolean enabled) {
        butFlyOff.setEnabled(enabled);
        clientgui.getMenuBar().setMoveFlyOffEnabled(enabled);
    }

    private void setEjectEnabled(boolean enabled) {
        butEject.setEnabled(enabled);
        clientgui.getMenuBar().setMoveEjectEnabled(enabled);
    }

    private void setUnjamEnabled(boolean enabled) {
        butRAC.setEnabled(enabled);
        clientgui.getMenuBar().setMoveUnjamEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled, boolean state) {
        if (state) {
            butSearchlight.setText(Messages
                    .getString("MovementDisplay.butSearchlightOff")); //$NON-NLS-1$
        } else {
            butSearchlight.setText(Messages
                    .getString("MovementDisplay.butSearchlightOn")); //$NON-NLS-1$
        }
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setMoveSearchlightEnabled(enabled);
    }

    private void setHullDownEnabled(boolean enabled) {
        butHullDown.setEnabled(enabled);
        clientgui.getMenuBar().setMoveHullDownEnabled(enabled);
    }

    private void setClearEnabled(boolean enabled) {
        butClear.setEnabled(enabled);
        clientgui.getMenuBar().setMoveClearEnabled(enabled);
    }

    private void setGetUpEnabled(boolean enabled) {
        butUp.setEnabled(enabled);
        clientgui.getMenuBar().setMoveGetUpEnabled(enabled);
    }

    private void setRaiseEnabled(boolean enabled) {
        butRaise.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRaiseEnabled(enabled);
    }

    private void setLowerEnabled(boolean enabled) {
        butLower.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLowerEnabled(enabled);
    }

    private void setRecklessEnabled(boolean enabled) {
        butReckless.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRecklessEnabled(enabled);
    }

    private void setAccEnabled(boolean enabled) {
        butAcc.setEnabled(enabled);
        clientgui.getMenuBar().setMoveAccEnabled(enabled);
    }

    private void setDecEnabled(boolean enabled) {
        butDec.setEnabled(enabled);
        clientgui.getMenuBar().setMoveDecEnabled(enabled);
    }

    private void setAccNEnabled(boolean enabled) {
        butAccN.setEnabled(enabled);
        clientgui.getMenuBar().setMoveAccNEnabled(enabled);
    }

    private void setDecNEnabled(boolean enabled) {
        butDecN.setEnabled(enabled);
        clientgui.getMenuBar().setMoveDecNEnabled(enabled);
    }

    private void setEvadeEnabled(boolean enabled) {
        butEvade.setEnabled(enabled);
        clientgui.getMenuBar().setMoveEvadeEnabled(enabled);
    }

    private void setShutdownEnabled(boolean enabled) {
        butShutdown.setEnabled(enabled);
        clientgui.getMenuBar().setMoveShutdownEnabled(enabled);
    }

    private void setStartupEnabled(boolean enabled) {
        butStartup.setEnabled(enabled);
        clientgui.getMenuBar().setMoveStartupEnabled(enabled);
    }

    private void setSelfDestructEnabled(boolean enabled) {
        butSelfDestruct.setEnabled(enabled);
        clientgui.getMenuBar().setMoveSelfDestructEnabled(enabled);
    }

    private void setEvadeAeroEnabled(boolean enabled) {
        butEvadeAero.setEnabled(enabled);
        clientgui.getMenuBar().setMoveEvadeAeroEnabled(enabled);
    }

    private void setRollEnabled(boolean enabled) {
        butRoll.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRollEnabled(enabled);
    }

    private void setLaunchEnabled(boolean enabled) {
        butLaunch.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLaunchEnabled(enabled);
    }

    private void setRecoverEnabled(boolean enabled) {
        butRecover.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRecoverEnabled(enabled);
    }

    private void setDropEnabled(boolean enabled) {
        butDrop.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveDropEnabled(enabled);
    }

    private void setJoinEnabled(boolean enabled) {
        butJoin.setEnabled(enabled);
        clientgui.getMenuBar().setMoveJoinEnabled(enabled);
    }

    private void setDumpEnabled(boolean enabled) {
        butDump.setEnabled(enabled);
        clientgui.getMenuBar().setMoveDumpEnabled(enabled);
    }

    private void setRamEnabled(boolean enabled) {
        butRam.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRamEnabled(enabled);
    }

    private void setHoverEnabled(boolean enabled) {
        butHover.setEnabled(enabled);
        clientgui.getMenuBar().setMoveHoverEnabled(enabled);
    }

    private void setTakeOffEnabled(boolean enabled) {
        butTakeOff.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveTakeOffEnabled(enabled);
    }

    private void setVTakeOffEnabled(boolean enabled) {
        butVTakeOff.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveVTakeOffEnabled(enabled);
    }

    private void setLandEnabled(boolean enabled) {
        butLand.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveLandEnabled(enabled);
    }

    private void setVLandEnabled(boolean enabled) {
        butVLand.setEnabled(enabled);
        //clientgui.getMenuBar().setMoveVLandEnabled(enabled);
    }

    private void setManeuverEnabled(boolean enabled) {
        butManeuver.setEnabled(enabled);
        clientgui.getMenuBar().setMoveManeuverEnabled(enabled);
    }

    private void setTurnLeftEnabled(boolean enabled) {
        butTurnLeft.setEnabled(enabled);
        clientgui.getMenuBar().setMoveTurnLeftEnabled(enabled);
    }

    private void setTurnRightEnabled(boolean enabled) {
        butTurnRight.setEnabled(enabled);
        clientgui.getMenuBar().setMoveTurnRightEnabled(enabled);
    }

    private void setThrustEnabled(boolean enabled) {
        butThrust.setEnabled(enabled);
        clientgui.getMenuBar().setMoveThrustEnabled(enabled);
    }

    private void setYawEnabled(boolean enabled) {
        butYaw.setEnabled(enabled);
        clientgui.getMenuBar().setMoveYawEnabled(enabled);
    }

    private void setEndOverEnabled(boolean enabled) {
        butEndOver.setEnabled(enabled);
        clientgui.getMenuBar().setMoveEndOverEnabled(enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

    private void updateTurnButton() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        setTurnEnabled(!ce.isImmobile() && !ce.isStuck()
                && ((ce.getWalkMP() > 0) || (ce.getJumpMP() > 0))
                && !(cmd.isJumping() && (ce instanceof Mech) && (ce.getJumpType() == Mech.JUMP_BOOSTER)));
    }
}
