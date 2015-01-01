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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
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
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.GameTurn;
import megamek.common.IEntityMovementMode;
import megamek.common.IEntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.ManeuverType;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Protomech;
import megamek.common.SpaceStation;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.TeleMissile;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class MovementDisplay extends StatusBarPhaseDisplay implements ActionListener,
DoneButtoned, KeyListener, GameListener, BoardViewListener {
    /**
     *
     */
    private static final long serialVersionUID = 9136822404087057673L;

    private static final int NUM_BUTTON_LAYOUTS = 4;

    public static final String MOVE_WALK = "moveWalk"; //$NON-NLS-1$
    public static final String MOVE_NEXT = "moveNext"; //$NON-NLS-1$
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
    // Aero Movement
    public static final String MOVE_ACC = "MoveAccelerate"; //$NON-NLS-1$
    public static final String MOVE_DEC = "MoveDecelerate"; //$NON-NLS-1$
    public static final String MOVE_EVADE_AERO = "MoveEvadeAero"; //$NON-NLS-1$
    public static final String MOVE_ACCN = "MoveAccNext"; //$NON-NLS-1$
    public static final String MOVE_DECN = "MoveDecNext"; //$NON-NLS-1$
    public static final String MOVE_ROLL = "MoveRoll"; //$NON-NLS-1$
    public static final String MOVE_LAUNCH = "MoveLaunch"; //$NON-NLS-1$
    public static final String MOVE_RECOVER = "MoveRecover"; //$NON-NLS-1$
    public static final String MOVE_DUMP = "MoveDump"; //$NON-NLS-1$
    public static final String MOVE_RAM = "MoveRam"; //$NON-NLS-1$
    public static final String MOVE_HOVER = "MoveHover"; //$NON-NLS-1$
    public static final String MOVE_MANEUVER = "MoveManeuver"; //$NON-NLS-1$
    public static final String MOVE_JOIN = "MoveJoin"; //$NON-NLS-1$
    // Aero Vector Movement
    public static final String MOVE_TURN_LEFT = "MoveTurnLeft"; //$NON-NLS-1$
    public static final String MOVE_TURN_RIGHT = "MoveTurnRight"; //$NON-NLS-1$
    public static final String MOVE_THRUST = "MoveThrust"; //$NON-NLS-1$
    public static final String MOVE_YAW = "MoveYaw"; //$NON-NLS-1$
    public static final String MOVE_END_OVER = "MoveEndOver"; //$NON-NLS-1$

    // parent game
    public Client client;
    private ClientGUI clientgui;

    // buttons
    private Panel panButtons;

    private Button butWalk;
    private Button butJump;
    private Button butBackup;
    private Button butTurn;
    private Button butSwim;

    private Button butUp;
    private Button butDown;
    private Button butCharge;
    private Button butDfa;

    private Button butRAC;
    private Button butFlee;
    private Button butEject;

    private Button butLoad;
    private Button butUnload;
    private Button butSpace;

    private Button butClear;

    private Button butNext;
    private Button butDone;
    private Button butMore;

    private Button butRaise;
    private Button butLower;

    private Button butSearchlight;

    private Button butLayMine;

    private Button butHullDown;

    private Button butClimbMode;

    private Button butDigIn;
    private Button butFortify;

    private Button butShakeOff;

    private Button butReckless;
    private Button butEvade;

    private Button butAcc;
    private Button butDec;
    private Button butEvadeAero;
    private Button butAccN;
    private Button butDecN;
    private Button butRoll;
    private Button butLaunch;
    private Button butRecover;
    private Button butDump;
    private Button butRam;
    private Button butHover;
    private Button butManeuver;
    private Button butJoin;

    private Button butTurnLeft;
    private Button butTurnRight;
    private Button butThrust;
    private Button butYaw;
    private Button butEndOver;

    private int buttonLayout;

    // order of buttons for various entity types
    private ArrayList<Button> buttonsMech;
    private ArrayList<Button> buttonsTank;
    private ArrayList<Button> buttonsVtol;
    private ArrayList<Button> buttonsInf;
    private ArrayList<Button> buttonsAero;

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
    private Vector<Entity> loadedUnits = null;

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
     * client.
     */
    public MovementDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        client = clientgui.getClient();
        client.game.addGameListener(this);

        gear = MovementDisplay.GEAR_LAND;

        shiftheld = false;

        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$

        butClear = new Button(Messages.getString("MovementDisplay.butClear")); //$NON-NLS-1$
        butClear.addActionListener(this);
        butClear.setEnabled(false);
        butClear.setActionCommand(MOVE_CLEAR);
        butClear.addKeyListener(this);

        butWalk = new Button(Messages.getString("MovementDisplay.butWalk")); //$NON-NLS-1$
        butWalk.addActionListener(this);
        butWalk.setEnabled(false);
        butWalk.setActionCommand(MOVE_WALK);
        butWalk.addKeyListener(this);

        butJump = new Button(Messages.getString("MovementDisplay.butJump")); //$NON-NLS-1$
        butJump.addActionListener(this);
        butJump.setEnabled(false);
        butJump.setActionCommand(MOVE_JUMP);
        butJump.addKeyListener(this);

        butSwim = new Button(Messages.getString("MovementDisplay.butSwim")); //$NON-NLS-1$
        butSwim.addActionListener(this);
        butSwim.setEnabled(false);
        butSwim.setActionCommand(MOVE_SWIM);
        butSwim.addKeyListener(this);

        butBackup = new Button(Messages.getString("MovementDisplay.butBackup")); //$NON-NLS-1$
        butBackup.addActionListener(this);
        butBackup.setEnabled(false);
        butBackup.setActionCommand(MOVE_BACK_UP);
        butBackup.addKeyListener(this);

        butTurn = new Button(Messages.getString("MovementDisplay.butTurn")); //$NON-NLS-1$
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
        butTurn.setActionCommand(MOVE_TURN);
        butTurn.addKeyListener(this);

        butUp = new Button(Messages.getString("MovementDisplay.butUp")); //$NON-NLS-1$
        butUp.addActionListener(this);
        butUp.setEnabled(false);
        butUp.setActionCommand(MOVE_GET_UP);
        butUp.addKeyListener(this);

        butDown = new Button(Messages.getString("MovementDisplay.butDown")); //$NON-NLS-1$
        butDown.addActionListener(this);
        butDown.setEnabled(false);
        butDown.setActionCommand(MOVE_GO_PRONE);
        butDown.addKeyListener(this);

        butCharge = new Button(Messages.getString("MovementDisplay.butCharge")); //$NON-NLS-1$
        butCharge.addActionListener(this);
        butCharge.setEnabled(false);
        butCharge.setActionCommand(MOVE_CHARGE);
        butCharge.addKeyListener(this);

        butDfa = new Button(Messages.getString("MovementDisplay.butDfa")); //$NON-NLS-1$
        butDfa.addActionListener(this);
        butDfa.setEnabled(false);
        butDfa.setActionCommand(MOVE_DFA);
        butDfa.addKeyListener(this);

        butFlee = new Button(Messages.getString("MovementDisplay.butFlee")); //$NON-NLS-1$
        butFlee.addActionListener(this);
        butFlee.setEnabled(false);
        butFlee.setActionCommand(MOVE_FLEE);
        butFlee.addKeyListener(this);

        butEject = new Button(Messages.getString("MovementDisplay.butEject")); //$NON-NLS-1$
        butEject.addActionListener(this);
        butEject.setEnabled(false);
        butEject.setActionCommand(MOVE_EJECT);
        butEject.addKeyListener(this);

        butRAC = new Button(Messages.getString("MovementDisplay.butRAC")); //$NON-NLS-1$
        butRAC.addActionListener(this);
        butRAC.setEnabled(false);
        butRAC.setActionCommand(MOVE_UNJAM);
        butRAC.addKeyListener(this);

        butSearchlight = new Button(Messages.getString("MovementDisplay.butSearchlightOn")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.setEnabled(false);
        butSearchlight.setActionCommand(MOVE_SEARCHLIGHT);
        butSearchlight.addKeyListener(this);

        butMore = new Button(Messages.getString("MovementDisplay.butMore")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.setEnabled(false);
        butMore.addKeyListener(this);

        butNext = new Button(Messages.getString("MovementDisplay.butNext")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(MOVE_NEXT);
        butNext.addKeyListener(this);

        butDone = new Button(Messages.getString("MovementDisplay.butDone")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);
        butDone.addKeyListener(this);

        butLoad = new Button(Messages.getString("MovementDisplay.butLoad")); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);
        butLoad.setActionCommand(MOVE_LOAD);
        butLoad.addKeyListener(this);

        butUnload = new Button(Messages.getString("MovementDisplay.butUnload")); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);
        butUnload.setActionCommand(MOVE_UNLOAD);
        butUnload.addKeyListener(this);

        butRaise = new Button(Messages.getString("MovementDisplay.butRaise")); //$NON-NLS-1$
        butRaise.addActionListener(this);
        butRaise.setEnabled(false);
        butRaise.setActionCommand(MOVE_RAISE_ELEVATION);
        butRaise.addKeyListener(this);

        butLower = new Button(Messages.getString("MovementDisplay.butLower")); //$NON-NLS-1$
        butLower.addActionListener(this);
        butLower.setEnabled(false);
        butLower.setActionCommand(MOVE_LOWER_ELEVATION);
        butLower.addKeyListener(this);

        butLayMine = new Button(Messages.getString("MovementDisplay.butLayMine")); //$NON-NLS-1$
        butLayMine.addActionListener(this);
        butLayMine.setEnabled(false);
        butLayMine.setActionCommand(MOVE_LAY_MINE);
        butLayMine.addKeyListener(this);

        butHullDown = new Button(Messages.getString("MovementDisplay.butHullDown")); //$NON-NLS-1$
        butHullDown.addActionListener(this);
        butHullDown.setEnabled(false);
        butHullDown.setActionCommand(MOVE_HULL_DOWN);
        butHullDown.addKeyListener(this);

        butClimbMode = new Button(Messages.getString("MovementDisplay.butClimbMode")); //$NON-NLS-1$
        butClimbMode.addActionListener(this);
        butClimbMode.setEnabled(false);
        butClimbMode.setActionCommand(MOVE_CLIMB_MODE);
        butClimbMode.addKeyListener(this);

        butDigIn = new Button(Messages.getString("MovementDisplay.butDigIn")); //$NON-NLS-1$
        butDigIn.addActionListener(this);
        butDigIn.setEnabled(false);
        butDigIn.setActionCommand(MOVE_DIG_IN);
        butDigIn.addKeyListener(this);

        butFortify = new Button(Messages.getString("MovementDisplay.butFortify")); //$NON-NLS-1$
        butFortify.addActionListener(this);
        butFortify.setEnabled(false);
        butFortify.setActionCommand(MOVE_FORTIFY);
        butFortify.addKeyListener(this);

        butShakeOff = new Button(Messages.getString("MovementDisplay.butShakeOff")); //$NON-NLS-1$
        butShakeOff.addActionListener(this);
        butShakeOff.setEnabled(false);
        butShakeOff.setActionCommand(MOVE_SHAKE_OFF);
        butShakeOff.addKeyListener(this);

        butReckless = new Button(Messages.getString("MovementDisplay.butReckless")); //$NON-NLS-1$
        butReckless.addActionListener(this);
        butReckless.setEnabled(false);
        butReckless.setActionCommand(MOVE_RECKLESS);
        butReckless.addKeyListener(this);

        butEvade = new Button(Messages.getString("MovementDisplay.butEvade")); //$NON-NLS-1$
        butEvade.addActionListener(this);
        butEvade.setEnabled(false);
        butEvade.setActionCommand(MOVE_EVADE);
        butEvade.addKeyListener(this);

        butAcc = new Button(Messages.getString("MovementDisplay.butAcc")); //$NON-NLS-1$
        butAcc.addActionListener(this);
        butAcc.setEnabled(false);
        butAcc.setActionCommand(MOVE_ACC);
        butAcc.addKeyListener(this);

        butDec = new Button(Messages.getString("MovementDisplay.butDec")); //$NON-NLS-1$
        butDec.addActionListener(this);
        butDec.setEnabled(false);
        butDec.setActionCommand(MOVE_DEC);
        butDec.addKeyListener(this);

        butAccN = new Button(Messages.getString("MovementDisplay.butAccN")); //$NON-NLS-1$
        butAccN.addActionListener(this);
        butAccN.setEnabled(false);
        butAccN.setActionCommand(MOVE_ACCN);
        butAccN.addKeyListener(this);

        butDecN = new Button(Messages.getString("MovementDisplay.butDecN")); //$NON-NLS-1$
        butDecN.addActionListener(this);
        butDecN.setEnabled(false);
        butDecN.setActionCommand(MOVE_DECN);
        butDecN.addKeyListener(this);

        butEvadeAero = new Button(Messages.getString("MovementDisplay.butEvadeAero")); //$NON-NLS-1$
        butEvadeAero.addActionListener(this);
        butEvadeAero.setEnabled(false);
        butEvadeAero.setActionCommand(MOVE_EVADE_AERO);
        butEvadeAero.addKeyListener(this);

        butRoll = new Button(Messages.getString("MovementDisplay.butRoll")); //$NON-NLS-1$
        butRoll.addActionListener(this);
        butRoll.setEnabled(false);
        butRoll.setActionCommand(MOVE_ROLL);
        butRoll.addKeyListener(this);

        butLaunch = new Button(Messages.getString("MovementDisplay.butLaunch")); //$NON-NLS-1$
        butLaunch.addActionListener(this);
        butLaunch.setEnabled(false);
        butLaunch.setActionCommand(MOVE_LAUNCH);
        butLaunch.addKeyListener(this);

        butRecover = new Button(Messages.getString("MovementDisplay.butRecover")); //$NON-NLS-1$
        butRecover.addActionListener(this);
        butRecover.setEnabled(false);
        butRecover.setActionCommand(MOVE_RECOVER);
        butRecover.addKeyListener(this);

        butDump = new Button(Messages.getString("MovementDisplay.butDump")); //$NON-NLS-1$
        butDump.addActionListener(this);
        butDump.setEnabled(false);
        butDump.setActionCommand(MOVE_DUMP);
        butDump.addKeyListener(this);

        butRam = new Button(Messages.getString("MovementDisplay.butRam")); //$NON-NLS-1$
        butRam.addActionListener(this);
        butRam.setEnabled(false);
        butRam.setActionCommand(MOVE_RAM);
        butRam.addKeyListener(this);

        butHover = new Button(Messages.getString("MovementDisplay.butHover")); //$NON-NLS-1$
        butHover.addActionListener(this);
        butHover.setEnabled(false);
        butHover.setActionCommand(MOVE_HOVER);
        butHover.addKeyListener(this);

        butManeuver = new Button(Messages.getString("MovementDisplay.butManeuver")); //$NON-NLS-1$
        butManeuver.addActionListener(this);
        butManeuver.setEnabled(false);
        butManeuver.setActionCommand(MOVE_MANEUVER);
        butManeuver.addKeyListener(this);

        butJoin = new Button(Messages.getString("MovementDisplay.butJoin")); //$NON-NLS-1$
        butJoin.addActionListener(this);
        butJoin.setEnabled(false);
        butJoin.setActionCommand(MOVE_JOIN);
        butJoin.addKeyListener(this);

        butTurnLeft = new Button(Messages.getString("MovementDisplay.butTurnLeft")); //$NON-NLS-1$
        butTurnLeft.addActionListener(this);
        butTurnLeft.setEnabled(false);
        butTurnLeft.setActionCommand(MOVE_TURN_LEFT);
        butTurnLeft.addKeyListener(this);

        butTurnRight = new Button(Messages.getString("MovementDisplay.butTurnRight")); //$NON-NLS-1$
        butTurnRight.addActionListener(this);
        butTurnRight.setEnabled(false);
        butTurnRight.setActionCommand(MOVE_TURN_RIGHT);
        butTurnRight.addKeyListener(this);

        butThrust = new Button(Messages.getString("MovementDisplay.butThrust")); //$NON-NLS-1$
        butThrust.addActionListener(this);
        butThrust.setEnabled(false);
        butThrust.setActionCommand(MOVE_THRUST);
        butThrust.addKeyListener(this);

        butYaw = new Button(Messages.getString("MovementDisplay.butYaw")); //$NON-NLS-1$
        butYaw.addActionListener(this);
        butYaw.setEnabled(false);
        butYaw.setActionCommand(MOVE_YAW);
        butYaw.addKeyListener(this);

        butEndOver = new Button(Messages.getString("MovementDisplay.butEndOver")); //$NON-NLS-1$
        butEndOver.addActionListener(this);
        butEndOver.setEnabled(false);
        butEndOver.setActionCommand(MOVE_END_OVER);
        butEndOver.addKeyListener(this);

        butSpace = new Button(".");
        butSpace.setEnabled(false);
        butSpace.addKeyListener(this);

        // add buttons to the lists, except space, more & next
        buttonsMech = new ArrayList<Button>(22);
        buttonsMech.add(butWalk);
        buttonsMech.add(butJump);
        buttonsMech.add(butBackup);
        buttonsMech.add(butTurn);
        buttonsMech.add(butUp);
        buttonsMech.add(butDown);
        buttonsMech.add(butCharge);
        buttonsMech.add(butDfa);
        buttonsMech.add(butLoad);
        buttonsMech.add(butUnload);
        buttonsMech.add(butClimbMode);
        buttonsMech.add(butSearchlight);
        buttonsMech.add(butHullDown);
        buttonsMech.add(butSwim);
        buttonsMech.add(butEvade);
        buttonsMech.add(butReckless);
        buttonsMech.add(butEject);
        buttonsMech.add(butFlee);
        buttonsMech.add(butRAC);
        // these are last, they won't be used by mechs
        buttonsMech.add(butFortify);
        buttonsMech.add(butLayMine);
        buttonsMech.add(butLower);
        buttonsMech.add(butRaise);

        buttonsTank = new ArrayList<Button>(22);
        buttonsTank.add(butWalk);
        buttonsTank.add(butBackup);
        buttonsTank.add(butTurn);
        buttonsTank.add(butLoad);
        buttonsTank.add(butUnload);
        buttonsTank.add(butCharge);
        buttonsTank.add(butClimbMode);
        buttonsTank.add(butSearchlight);
        buttonsTank.add(butHullDown);
        buttonsTank.add(butSwim);
        buttonsTank.add(butEvade);
        buttonsTank.add(butReckless);
        buttonsTank.add(butEject);
        buttonsTank.add(butFlee);
        buttonsTank.add(butRAC);
        buttonsTank.add(butLayMine);
        buttonsTank.add(butShakeOff);
        // these are last, they won't be used by tanks
        buttonsTank.add(butUp);
        buttonsTank.add(butDown);
        buttonsTank.add(butJump);
        buttonsTank.add(butDigIn);
        buttonsTank.add(butLower);
        buttonsTank.add(butRaise);

        buttonsVtol = new ArrayList<Button>(22);
        buttonsVtol.add(butWalk);
        buttonsVtol.add(butBackup);
        buttonsVtol.add(butLower);
        buttonsVtol.add(butRaise);
        buttonsVtol.add(butTurn);
        buttonsVtol.add(butLoad);
        buttonsVtol.add(butUnload);
        buttonsVtol.add(butSearchlight);
        buttonsVtol.add(butEvade);
        buttonsVtol.add(butReckless);
        buttonsVtol.add(butEject);
        buttonsVtol.add(butFlee);
        buttonsVtol.add(butRAC);
        buttonsVtol.add(butShakeOff);
        // these are last, they won't be used by vtol
        buttonsVtol.add(butLayMine);
        buttonsVtol.add(butSwim);
        buttonsVtol.add(butClimbMode);
        buttonsVtol.add(butCharge);
        buttonsVtol.add(butDfa);
        buttonsVtol.add(butUp);
        buttonsVtol.add(butDown);
        buttonsVtol.add(butJump);
        buttonsVtol.add(butDigIn);

        buttonsInf = new ArrayList<Button>(22);
        buttonsInf.add(butWalk);
        buttonsInf.add(butJump);
        buttonsInf.add(butLower);
        buttonsInf.add(butRaise);
        buttonsInf.add(butTurn);
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
        // these are last, they won't be used by infantry
        buttonsInf.add(butLoad);
        buttonsInf.add(butUnload);
        buttonsInf.add(butBackup);
        buttonsInf.add(butHullDown);
        buttonsInf.add(butCharge);
        buttonsInf.add(butDfa);
        buttonsInf.add(butUp);
        buttonsInf.add(butDown);

        buttonsAero = new ArrayList<Button>(22);
        if (!client.game.useVectorMove()) {
            buttonsAero.add(butWalk);
            buttonsAero.add(butAcc);
            buttonsAero.add(butDec);
            buttonsAero.add(butTurn);
            buttonsAero.add(butAccN);
            buttonsAero.add(butDecN);
            buttonsAero.add(butRoll);
            buttonsAero.add(butEvadeAero);
            buttonsAero.add(butRam);
            buttonsAero.add(butLower);
            buttonsAero.add(butRaise);
            buttonsAero.add(butManeuver);
            buttonsAero.add(butEject);
            buttonsAero.add(butFlee);
            buttonsAero.add(butLaunch);
            buttonsAero.add(butRecover);
            buttonsAero.add(butJoin);
            buttonsAero.add(butHover);
            buttonsAero.add(butRAC);
            buttonsAero.add(butDump);
            // not used
            buttonsAero.add(butHullDown);
            buttonsAero.add(butShakeOff);
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
            buttonsAero.add(butFlee);
            buttonsAero.add(butLaunch);
            buttonsAero.add(butRecover);
            buttonsAero.add(butJoin);
            buttonsAero.add(butRAC);
            buttonsAero.add(butDump);
            // not used
            buttonsAero.add(butUp);
            buttonsAero.add(butDown);
            buttonsAero.add(butJump);
            buttonsAero.add(butDigIn);
            buttonsAero.add(butFortify);
            buttonsAero.add(butHullDown);
            buttonsAero.add(butShakeOff);
        }
        // TODO: allow Aeros to taxi if on the ground

        // layout button grid
        panButtons = new Panel();
        buttonLayout = 0;
        setupButtonPanel();

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // addBag(clientgui.bv, gridbag, c);

        // c.weightx = 1.0; c.weighty = 0;
        // c.gridwidth = 1;
        // addBag(client.cb.getComponent(), gridbag, c);

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

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));

        // choose button order based on entity type
        ArrayList<Button> buttonList = buttonsMech;
        final Entity ce = ce();
        if (ce != null) {
            if (ce instanceof Infantry) {
                buttonList = buttonsInf;
            } else if (ce instanceof VTOL) {
                buttonList = buttonsVtol;
            } else if (ce instanceof Tank) {
                buttonList = buttonsTank;
            } else if (ce instanceof Aero) {
                buttonList = buttonsAero;
            }
        }
        // should this layout be skipped? (if nothing enabled)
        boolean ok = false;
        while (!ok && (buttonLayout != 0)) {
            for (int i = buttonLayout * 6; (i < (buttonLayout + 1) * 6) && (i < buttonList.size()); i++) {
                if (buttonList.get(i).isEnabled()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // skip as nothing was enabled
                buttonLayout++;
                if (buttonLayout * 6 >= buttonList.size()) {
                    buttonLayout = 0;
                }
            }
        }

        panButtons.add(butNext);
        for (int i = buttonLayout * 6; (i < (buttonLayout + 1) * 6) && (i < buttonList.size()); i++) {
            panButtons.add(buttonList.get(i));
        }
        panButtons.add(butMore);
        panButtons.validate();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public synchronized void selectEntity(int en) {
        final Entity ce = client.game.getEntity(en);

        // hmm, sometimes this gets called when there's no ready entities?
        if (ce == null) {
            System.err.println("MovementDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }

        cen = en;
        clientgui.setSelectedEntityNum(en);

        clearAllMoves();
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
        final boolean isMech = (ce instanceof Mech);
        final boolean isInfantry = (ce instanceof Infantry);
        //final boolean isProtomech = (ce instanceof Protomech);
        final boolean isAero = (ce instanceof Aero);
        // ^-- I suppose these should really be methods, a-la
        // Entity.canCharge(), Entity.canDFA()...

        setWalkEnabled(!ce.isImmobile()
                && ((ce.getWalkMP() > 0) || (ce.getRunMP() > 0)) && !ce.isStuck());
        setJumpEnabled(!isAero && !ce.isImmobile() && (ce.getJumpMP() > 0) && !(ce.isStuck() && !ce.canUnstickByJumping()));
        setSwimEnabled(!isAero && !ce.isImmobile() && ce.hasUMU() && client.game.getBoard().getHex(ce.getPosition()).containsTerrain(Terrains.WATER));
        setBackUpEnabled(butWalk.isEnabled() && !isAero);

        setChargeEnabled(ce.canCharge());
        setDFAEnabled(ce.canDFA());
        setRamEnabled(ce.canRam());

        if (isInfantry) {
            if (client.game.containsMinefield(ce.getPosition())) {
                setClearEnabled(true);
            } else {
              setClearEnabled(false);
            }
        } else {
            setClearEnabled(false);
        }

        if ((ce.getMovementMode() == IEntityMovementMode.HYDROFOIL) || (ce.getMovementMode() == IEntityMovementMode.NAVAL) || (ce.getMovementMode() == IEntityMovementMode.SUBMARINE) || (ce.getMovementMode() == IEntityMovementMode.INF_UMU) || (ce.getMovementMode() == IEntityMovementMode.VTOL) || (ce.getMovementMode() == IEntityMovementMode.AIRMECH) || (ce.getMovementMode() == IEntityMovementMode.AEROSPACE) || (ce.getMovementMode() == IEntityMovementMode.BIPED_SWIM) || (ce.getMovementMode() == IEntityMovementMode.QUAD_SWIM)) {
            butClimbMode.setEnabled(false);
        } else {
            butClimbMode.setEnabled(true);
        }

        if (isInfantry) {
            butDigIn.setEnabled(true);
            butFortify.setEnabled(true);
        } else {
            butDigIn.setEnabled(false);
            butFortify.setEnabled(false);
        }
        setTurnEnabled(!ce.isImmobile() && !ce.isStuck() && ((ce.getWalkMP() > 0) || (ce.getJumpMP() > 0)));
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateLoadButtons();
        updateElevationButtons();

        setEvadeEnabled((isMech || (ce instanceof Tank)) && client.game.getOptions().booleanOption("tacops_evade"));

        updateRecoveryButton();
        updateJoinButton();
        updateDumpButton();

        if (ce instanceof Aero) {
            butThrust.setEnabled(true);
            butYaw.setEnabled(true);
            butEndOver.setEnabled(true);
            butTurnLeft.setEnabled(true);
            butTurnRight.setEnabled(true);
            setEvadeAeroEnabled(true);
            setEjectEnabled(true);
            // no turning for spheroids in atmosphere
            if ((((Aero) ce).isSpheroid() || client.game.getPlanetaryConditions().isVacuum())
                    && client.game.getBoard().inAtmosphere()) {
                setTurnEnabled(false);
            }
            //jumpships and space stations can turn under different conditions
            if((ce instanceof Jumpship) && !(ce instanceof Warship) && !(ce instanceof SpaceStation)) {
                setTurnEnabled(true);
            }
            if((ce instanceof SpaceStation) && (ce.getRunMP() > 0)) {
                setTurnEnabled(true);
            }
        }

        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();
        updateFleeButton();
        updateLaunchButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();

        if (isInfantry && ce.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_VIBROSHOVEL)) {
            butFortify.setEnabled(true);
        } else {
            butFortify.setEnabled(false);
        }

        if (isInfantry && client.game.getOptions().booleanOption("tacops_dig_in")) {
            butDigIn.setEnabled(true);
        } else {
            butDigIn.setEnabled(false);
        }

        butShakeOff.setEnabled((ce instanceof Tank) && (ce.getSwarmAttackerId() != Entity.NONE));

        setLayMineEnabled(ce.canLayMine());

        setFleeEnabled(ce.canFlee());
        if (client.game.getOptions().booleanOption("vehicles_can_eject")) { //$NON-NLS-1$
            setEjectEnabled((!isInfantry) && !(isMech && (((Mech) ce).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) && ce.isActive());
        } else {
            setEjectEnabled(isMech && (((Mech) ce).getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED) && ce.isActive());
        }
        setupButtonPanel();

        // handle land air mech transformations
        updateTransformationButtons(ce);
    }

    /**
     * This funtcion is called to update the transformation buttons.
     *
     * @param ce
     *            the current entity
     */
    private void updateTransformationButtons(Entity ce) {
        if (ce instanceof LandAirMech) {
            final LandAirMech lam = (LandAirMech) ce;

            setLAMmechModeEnabled(lam.canConvertToMech());
            setLAMairmechModeEnabled(lam.canConvertToAirmech());
            setLAMaircraftModeEnabled(lam.canConvertToAircraft());
        } else {
            setLAMmechModeEnabled(false);
            setLAMairmechModeEnabled(false);
            setLAMaircraftModeEnabled(false);
        }
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getString("MovementDisplay.its_your_turn")); //$NON-NLS-1$
        selectEntity(client.getFirstEntityNum());
        butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
        butDone.setEnabled(true);
        setNextEnabled(true);
        butMore.setEnabled(true);
        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private synchronized void endMyTurn() {
        final Entity ce = ce();

        // end my turn, then.
        disableButtons();
        Entity next = client.game.getNextEntity(client.game.getTurnIndex());
        if ((IGame.Phase.PHASE_MOVEMENT == client.game.getPhase()) && (null != next) && (null != ce) && (next.getOwnerId() != ce.getOwnerId())) {
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
        setEjectEnabled(false);
        setUnjamEnabled(false);
        setSearchlightEnabled(false, false);
        setGetUpEnabled(false);
        setGoProneEnabled(false);
        setChargeEnabled(false);
        setDFAEnabled(false);
        setNextEnabled(false);
        butMore.setEnabled(false);
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setClearEnabled(false);
        setHullDownEnabled(false);
        setSwimEnabled(false);
        setEvadeEnabled(false);
        setAccEnabled(false);
        setDecEnabled(false);
        setEvadeAeroEnabled(false);
        setAccNEnabled(false);
        setDecNEnabled(false);
        setRollEnabled(false);
        setLaunchEnabled(false);
        setThrustEnabled(false);
        setYawEnabled(false);
        setEndOverEnabled(false);
        setTurnLeftEnabled(false);
        setTurnRightEnabled(false);
        setDumpEnabled(false);
        setRamEnabled(false);
        setHoverEnabled(false);
        setManeuverEnabled(false);
        setRecklessEnabled(false);
        setJoinEnabled(false);
    }

    /**
     * Clears out the curently selected movement data and resets it.
     */
    private void clearAllMoves() {
        final Entity ce = ce();

        // switch back from swimming to normal mode.
        if (ce.getMovementMode() == IEntityMovementMode.BIPED_SWIM) {
            ce.setMovementMode(IEntityMovementMode.BIPED);
        } else if (ce.getMovementMode() == IEntityMovementMode.QUAD_SWIM) {
            ce.setMovementMode(IEntityMovementMode.QUAD);
        }

        // clear board cursors
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);

        // create new current and considered paths
        cmd = new MovePath(client.game, ce);

        // set to "walk," or the equivalent
        gear = MovementDisplay.GEAR_LAND;

        // update some GUI elements
        clientgui.bv.clearMovementData();
        butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateElevationButtons();
        updateFleeButton();
        updateLaunchButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();

        loadedUnits = ce.getLoadedUnits();

        updateLoadButtons();
        updateElevationButtons();
        updateRecoveryButton();
        updateJoinButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();
    }

    private void removeLastStep() {
        cmd.removeLastStep();

        if (cmd.length() == 0) {
            clearAllMoves();
        } else {
            clientgui.bv.drawMovementData(ce(), cmd);

            // Set the button's label to "Done"
            // if the entire move is impossible.
            MovePath possible = cmd.clone();
            possible.clipToPossible();
            if (possible.length() == 0) {
                butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
            }
        }
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private synchronized void moveTo(MovePath md) {

        final Entity ce = ce();
        boolean dontCheckPSR = false;
        if (ce instanceof Aero) {

            Aero a = (Aero) ce;

            // first check for stalling
            if (client.game.getBoard().inAtmosphere() && !a.isVSTOL() && !a.isSpheroid()  && !client.game.getPlanetaryConditions().isVacuum()
                    && (((md == null) && (a.getCurrentVelocity() == 0)) || ((md != null) && (md.getFinalVelocity() == 0)))) {

                // add a stall to the movement path
                md.addStep(MovePath.STEP_STALL);
            }

            // should check to see if md is null. If so I need to check and see
            // if the units
            // current velocity is zero
            if (md != null) {

                boolean isRamming = false;
                if ((md.getLastStep() != null) && (md.getLastStep().getType() == MovePath.STEP_RAM)) {
                    isRamming = true;
                }

                // if using advanced movement then I need to add on movement
                // steps
                // to get the vessel from point a to point b
                // if the unit is ramming then this is already done
                if (client.game.useVectorMove() && !isRamming) {
                    // get rid of any illegal moves or the rest won't be added
                    md.clipToPossible();
                    md = addSteps(md, ce);

                }

                // check to see if the unit is out of control. If so, then set
                // up a new
                // move path for them.
                // the same should be true if the unit is shutdown or the pilot
                // is unconscious
                if (!client.game.useVectorMove() && (a.isOutControlTotal() || a.isShutDown() || a.getCrew().isUnconscious())) {

                    MovePath oldmd = md;

                    dontCheckPSR = true;
                    md = new MovePath(client.game, ce);
                    int vel = a.getCurrentVelocity();

                    // need to check for stall here as well
                    if ((vel == 0) && !(a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum()) && client.game.getBoard().inAtmosphere() && !a.isVSTOL()) {
                        // add a stall to the movement path
                        md.addStep(MovePath.STEP_STALL);
                    }

                    while (vel > 0) {
                        // check to see if the unit is currently on a border
                        // and facing a direction that would indicate leaving
                        // the map
                        Coords position = a.getPosition();
                        int facing = a.getFacing();
                        MoveStep step = md.getLastStep();
                        if (step != null) {
                            position = step.getPosition();
                            facing = step.getFacing();
                        }
                        boolean evenx = (position.x % 2) == 0;
                        if ((((position.x == 0) && ((facing == 5) || (facing == 4))) || ((position.x == client.game.getBoard().getWidth() - 1) && ((facing == 1) || (facing == 2))) || ((position.y == 0) && ((facing == 1) || (facing == 5) || (facing == 0)) && evenx) || ((position.y == 0) && (facing == 0)) || ((position.y == client.game.getBoard().getHeight() - 1) && ((facing == 2) || (facing == 3) || (facing == 4)) && !evenx) || ((position.y == client.game.getBoard().getHeight() - 1) && (facing == 3)))) {
                            // then this birdie go bye-bye
                            // set the conditions for removal
                            md.addStep(MovePath.STEP_OFF);
                            vel = 0;

                        } else {

                            if (a.isRandomMove()) {
                                int roll = Compute.d6(1);
                                switch (roll) {
                                case 1:
                                    md.addStep(MovePath.STEP_FORWARDS);
                                    md.addStep(MovePath.STEP_TURN_LEFT);
                                    md.addStep(MovePath.STEP_TURN_LEFT);
                                case 2:
                                    md.addStep(MovePath.STEP_FORWARDS);
                                    md.addStep(MovePath.STEP_TURN_LEFT);
                                case 3:
                                case 4:
                                    md.addStep(MovePath.STEP_FORWARDS);
                                case 5:
                                    md.addStep(MovePath.STEP_FORWARDS);
                                    md.addStep(MovePath.STEP_TURN_RIGHT);
                                case 6:
                                    md.addStep(MovePath.STEP_FORWARDS);
                                    md.addStep(MovePath.STEP_TURN_RIGHT);
                                    md.addStep(MovePath.STEP_TURN_RIGHT);
                                }
                            } else {
                                md.addStep(MovePath.STEP_FORWARDS);
                            }

                            vel--;
                        }

                    }

                    // check to see if old movement path contained a launch and
                    // we are still on the board
                    if (oldmd.contains(MovePath.STEP_LAUNCH) && !md.contains(MovePath.STEP_OFF)) {
                        // since launches have to be the last step
                        MoveStep lastStep = oldmd.getLastStep();
                        if (lastStep.getType() == MovePath.STEP_LAUNCH) {
                            md.addStep(lastStep.getType(), lastStep.getLaunched());
                        }
                    }

                } else {

                    // I think this should be ok now
                    md.clipToPossible();
                    // check to see if velocity left is zero
                    MoveStep step = md.getLastStep();
                    if (step != null) {
                        if ((step.getVelocityLeft() > 0) && !client.game.useVectorMove() && (step.getType() != MovePath.STEP_FLEE)) {
                            // pop up some dialog telling the unit that it did
                            // not spend enough
                            String title = Messages.getString("MovementDisplay.VelocityLeft.title"); //$NON-NLS-1$
                            String body = Messages.getString("MovementDisplay.VelocityLeft.message"); //$NON-NLS-1$
                            clientgui.doAlertDialog(title, body);
                            return;
                        }
                    } else {
                        // if the step is null then the unit didn't move. Make
                        // sure velocity is zero
                        if ((a.getCurrentVelocity() > 0) && !client.game.useVectorMove()) {
                            // pop up some dialog telling the unit that it did
                            // not spend enough
                            String title = Messages.getString("MovementDisplay.VelocityLeft.title"); //$NON-NLS-1$
                            String body = Messages.getString("MovementDisplay.VelocityLeft.message"); //$NON-NLS-1$
                            clientgui.doAlertDialog(title, body);
                            return;
                        }
                    }
                }

                // check for G-forces (not for vectored movement)
                String check = SharedUtility.doThrustCheck(md);
                if (!client.game.useVectorMove() && (check.length() > 0) && GUIPreferences.getInstance().getNagForPSR()) {
                    ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages.getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
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
            }
        }

        md.clipToPossible();
        if ((md.length() == 0) && GUIPreferences.getInstance().getNagForNoAction()) {
            // Hmm....no movement steps, comfirm this action
            String title = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.title"); //$NON-NLS-1$
            String body = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        if (md.hasActiveMASC() && GUIPreferences.getInstance().getNagForMASC()) {
            // pop up are you sure dialog
            Mech m = (Mech) ce();
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages.getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ConfirmMoveRoll", new Object[] { new Integer(m.getMASCTarget()) }), //$NON-NLS-1$
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

        String check = SharedUtility.doPSRCheck(md, client);
        if ((check.length() > 0) && GUIPreferences.getInstance().getNagForPSR() && !dontCheckPSR) {
            ConfirmDialog nag = new ConfirmDialog(clientgui.frame, Messages.getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
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

        disableButtons();
        clientgui.bv.clearMovementData();
        if (ce().hasUMU() || (ce() instanceof LandAirMech)) {
            client.sendUpdateEntity(ce());
        }
        client.moveEntity(cen, md);
    }

    /**
     * Returns the current entity.
     */
    private synchronized Entity ce() {
        return client.game.getEntity(cen);
    }

    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if (shiftheld || (gear == GEAR_TURN)) {
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false);
        } else if ((gear == GEAR_LAND) || (gear == GEAR_JUMP)) {
            cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
        } else if (gear == GEAR_BACKUP) {
            cmd.findPathTo(dest, MovePath.STEP_BACKWARDS);
        } else if (gear == GEAR_CHARGE) {
            cmd.findPathTo(dest, MovePath.STEP_CHARGE);
        } else if (gear == GEAR_DFA) {
            cmd.findPathTo(dest, MovePath.STEP_DFA);
        } else if (gear == GEAR_SWIM) {
            cmd.findPathTo(dest, MovePath.STEP_SWIM);
        } else if (gear == GEAR_RAM) {
            cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
        } else if (gear == GEAR_IMMEL) {
            cmd.addStep(MovePath.STEP_UP, true, true);
            cmd.addStep(MovePath.STEP_UP, true, true);
            cmd.addStep(MovePath.STEP_DEC, true, true);
            cmd.addStep(MovePath.STEP_DEC, true, true);
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), true);
            gear = GEAR_LAND;
        } else if (gear == GEAR_SPLIT_S) {
            cmd.addStep(MovePath.STEP_DOWN, true, true);
            cmd.addStep(MovePath.STEP_DOWN, true, true);
            cmd.addStep(MovePath.STEP_ACC, true, true);
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
        boolean nopath = (ce instanceof Aero) && client.game.useVectorMove();

        // ignore buttons other than 1
        if (!client.isMyTurn() || ((b.getModifiers() & InputEvent.BUTTON1_MASK) == 0)) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_MASK) != 0) || ((b.getModifiers() & InputEvent.ALT_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;
        }

        if ((b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && !nopath) {
            if (!b.getCoords().equals(clientgui.getBoardView().getLastCursor()) || shiftheld || (gear == MovementDisplay.GEAR_TURN)) {
                clientgui.getBoardView().cursor(b.getCoords());

                // either turn or move
                if (ce != null) {
                    currentMove(b.getCoords());
                    clientgui.bv.drawMovementData(ce, cmd);
                }
            }
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {

            Coords moveto = b.getCoords();
            clientgui.bv.drawMovementData(ce, cmd);

            clientgui.getBoardView().select(b.getCoords());

            if (shiftheld || (gear == MovementDisplay.GEAR_TURN)) {
                butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$

                // Set the button's label to "Done"
                // if the entire move is impossible.
                MovePath possible = cmd.clone();
                possible.clipToPossible();
                if (possible.length() == 0) {
                    butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
                }
                return;
            }

            if (gear == MovementDisplay.GEAR_RAM) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce) || !(target instanceof Aero)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantRam"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clearAllMoves();
                    return;
                }

                // check if it's a valid ram
                // First I need to add moves to the path if advanced
                if ((ce instanceof Aero) && client.game.useVectorMove()) {
                    cmd.clipToPossible();
                    cmd = addSteps(cmd, ce);
                }

                cmd.addStep(MovePath.STEP_RAM);

                ToHitData toHit = new RamAttackAction(cen, target.getTargetType(), target.getTargetId(), target.getPosition()).toHit(client.game, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {

                    // Determine how much damage the charger will take.
                    Aero ta = (Aero) target;
                    Aero ae = (Aero) ce;
                    int toAttacker = RamAttackAction.getDamageTakenBy(ae, ta, cmd.getSecondFinalPosition(ae.getPosition()), cmd.getHexesMoved(), ta.getCurrentVelocity());
                    int toDefender = RamAttackAction.getDamageFor(ae, ta, cmd.getSecondFinalPosition(ae.getPosition()), cmd.getHexesMoved(), ta.getCurrentVelocity());

                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RamDialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                            Messages.getString("MovementDisplay.RamDialog.message", new Object[] { //$NON-NLS-1$
                                    toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())), toHit.getDesc(), new Integer(toDefender), toHit.getTableDesc(), new Integer(toAttacker) }))) {
                        // if they answer yes, charge the target.
                        cmd.getLastStep().setTarget(target);
                        moveTo(cmd);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    }
                    return;
                }
                // if not valid, tell why
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantRam"), //$NON-NLS-1$
                        toHit.getDesc());
                clearAllMoves();
                return;
            } else if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clearAllMoves();
                    return;
                }

                ToHitData toHit = new ChargeAttackAction(cen, target.getTargetType(), target.getTargetId(), target.getPosition()).toHit(client.game, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {

                    // Determine how much damage the charger will take.
                    int toAttacker = 0;
                    if (target.getTargetType() == Targetable.TYPE_ENTITY) {
                        Entity te = (Entity) target;
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce, te, client.game.getOptions().booleanOption("tacops_charge_damage"), cmd.getHexesMoved()); //$NON-NLS-1$
                    } else if ((target.getTargetType() == Targetable.TYPE_FUEL_TANK) || (target.getTargetType() == Targetable.TYPE_BUILDING)) {
                        Building bldg = client.game.getBoard().getBuildingAt(moveto);
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce, bldg, moveto);
                    }

                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ChargeDialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                            Messages.getString("MovementDisplay.ChargeDialog.message", new Object[] { //$NON-NLS-1$
                                    toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())), toHit.getDesc(), new Integer(ChargeAttackAction.getDamageFor(ce, client.game.getOptions().booleanOption("tacops_charge_damage"), cmd.getHexesMoved())), toHit.getTableDesc(), new Integer(toAttacker) }))) {
                        // if they answer yes, charge the target.
                        cmd.getLastStep().setTarget(target);
                        moveTo(cmd);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    }
                    return;
                }
                // if not valid, tell why
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"), //$NON-NLS-1$
                        toHit.getDesc());
                clearAllMoves();
                return;
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if target is valid
                final Targetable target = chooseTarget(b.getCoords());
                if ((target == null) || target.equals(ce)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
                    clearAllMoves();
                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = DfaAttackAction.toHit(client.game, cen, target, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.DFADialog.title", new Object[] { target.getDisplayName() }), //$NON-NLS-1$
                            Messages.getString("MovementDisplay.DFADialog.message", new Object[] { //$NON-NLS-1$
                                    toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())), toHit.getDesc(), new Integer(DfaAttackAction.getDamageFor(ce, (target instanceof Infantry) && !(target instanceof BattleArmor))), toHit.getTableDesc(), new Integer(DfaAttackAction.getDamageTakenBy(ce)) }))) {
                        // if they answer yes, DFA the target
                        cmd.getLastStep().setTarget(target);
                        moveTo(cmd);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    }
                    return;

                }
                // if not valid, tell why
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"), //$NON-NLS-1$
                        toHit.getDesc());
                clearAllMoves();
                return;
            }

            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
            updateProneButtons();
            updateRACButton();
            updateSearchlightButton();
            updateLoadButtons();
            updateElevationButtons();
            updateFleeButton();
            updateLaunchButton();
            updateRecklessButton();
            updateHoverButton();
            updateManeuverButton();
            updateSpeedButtons();
            updateThrustButton();
            updateRollButton();
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
                    setGetUpEnabled(!ce.isImmobile() && !ce.isStuck() && !((Mech)ce).cannotStandUpFromHullDown());
                } else {
                    setGetUpEnabled(!ce.isImmobile() && !ce.isStuck());
                }
                setGoProneEnabled(!ce.isImmobile() && isMech && !ce.isStuck());
                setHullDownEnabled(false);
            } else {
                setGetUpEnabled(false);
                setGoProneEnabled(!ce.isImmobile() && isMech && !ce.isStuck() && !(butUp.isEnabled()));
                setHullDownEnabled(ce.canGoHullDown());
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
        setUnjamEnabled(ce.canUnjamRAC() && ((gear == MovementDisplay.GEAR_LAND) || (gear == MovementDisplay.GEAR_TURN) || (gear == MovementDisplay.GEAR_BACKUP)) && (cmd.getMpUsed() <= ce.getWalkMP()));
    }

    private void updateSearchlightButton() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }
        setSearchlightEnabled(ce.hasSpotlight() && !cmd.contains(MovePath.STEP_SEARCHLIGHT), ce().isUsingSpotlight());
    }

    private synchronized void updateElevationButtons() {
        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if ((ce instanceof Aero) && ((Aero) ce).isOutControlTotal()) {
            setRaiseEnabled(false);
            setLowerEnabled(false);
            return;
        }

        setRaiseEnabled(ce.canGoUp(cmd.getFinalElevation(), cmd.getFinalCoords()));
        setLowerEnabled(ce.canGoDown(cmd.getFinalElevation(), cmd.getFinalCoords()));
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

        if (client.game.getBoard().inAtmosphere()) {
            setRollEnabled(false);
        }

        if (cmd.contains(MovePath.STEP_ROLL)) {
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

        if (!(a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum())) {
            return;
        }

        if (!client.game.getBoard().inAtmosphere()) {
            return;
        }

        if (!cmd.contains(MovePath.STEP_HOVER)) {
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

        if (mpUsed >= a.getRunMP()) {
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
        } else if (last.getType() == MovePath.STEP_ACC) {
            setAccEnabled(true);
        } else if ((last.getType() == MovePath.STEP_DEC) && (vel > 0)) {
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
        if (!cmd.contains(MovePath.STEP_ACC) && !cmd.contains(MovePath.STEP_DEC) && !cmd.contains(MovePath.STEP_DECN)) {
            setAccNEnabled(true);
        }

        if (!cmd.contains(MovePath.STEP_ACC) && !cmd.contains(MovePath.STEP_DEC) && !cmd.contains(MovePath.STEP_ACCN) && (veln > 0)) {
            setDecNEnabled(true);
        }

        // acc/dec next needs to be disabled if acc/dec used before a failed
        // maneuver
        if (a.didFailManeuver() && a.didAccDecNow()) {
            setDecNEnabled(false);
            setAccNEnabled(false);
        }


        //if in atmosphere, limit acceleration to 2x safe thrust
        if(!client.game.getBoard().inSpace() && (vel == (2 * a.getWalkMP()))) {
            setAccEnabled(false);
        }
        //velocity next will get halved before next turn so allow up to 4 times
        if(!client.game.getBoard().inSpace() && (veln == (4 * a.getWalkMP()))) {
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
        if (ce() instanceof Aero) {
            Aero a = (Aero) ce();
            setDumpEnabled(a.hasBombs() && cmd.length() == 0);
        } else {
            setDumpEnabled(false);
        }
    */
    }

    private void updateFleeButton() {
        final Entity ce = ce();

        // Aeros should be able to flee if they reach a border hex with velocity
        // remaining
        // and facing the right direction
        if (ce instanceof Aero) {
            MoveStep step = cmd.getLastStep();
            Coords position = ce.getPosition();
            int facing = ce.getFacing();
            Aero a = (Aero) ce;
            int velocityLeft = a.getCurrentVelocity();
            if (step != null) {
                position = step.getPosition();
                facing = step.getFacing();
                velocityLeft = step.getVelocityLeft();
            }
            boolean evenx = (position.x % 2) == 0;
            if ((velocityLeft > 0) && (((position.x == 0) && ((facing == 5) || (facing == 4))) || ((position.x == client.game.getBoard().getWidth() - 1) && ((facing == 1) || (facing == 2))) || ((position.y == 0) && ((facing == 1) || (facing == 5) || (facing == 0)) && evenx) || ((position.y == 0) && (facing == 0)) || ((position.y == client.game.getBoard().getHeight() - 1) && ((facing == 2) || (facing == 3) || (facing == 4)) && !evenx) || ((position.y == client.game.getBoard().getHeight() - 1) && (facing == 3)))) {
                setFleeEnabled(true);
            } else {
                setFleeEnabled(false);
            }
        }
    }

    private void updateLaunchButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        setLaunchEnabled((ce.getLaunchableFighters().size() > 0) || (ce.getLaunchableSmallCraft().size() > 0));

    }

    private void updateRecklessButton() {

        final Entity ce = ce();

        if (null == ce) {
            return;
        }

        if(ce instanceof Protomech) {
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

        if(client.game.getBoard().inSpace() || client.game.getBoard().onGround()) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        if (a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum()) {
            return;
        }

        if (!a.didFailManeuver() && ((null == cmd) || !cmd.contains(MovePath.STEP_MANEUVER))) {
            setManeuverEnabled(true);
        }
        return;
    }

    private synchronized void updateLoadButtons() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        boolean legalGear = ((gear == MovementDisplay.GEAR_LAND) || (gear == MovementDisplay.GEAR_TURN) || (gear == MovementDisplay.GEAR_BACKUP));
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
        if (!legalGear || (loadedUnits.size() == 0) || (cen == Entity.NONE) || (!canUnloadHere)) {
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
            Enumeration<Entity> entities = client.game.getEntities(ce.getPosition());
            boolean isGood = false;
            while (entities.hasMoreElements()) {
                // Is the other unit friendly and not the current entity?
                other = entities.nextElement();
                if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                    // Yup. If the current entity has at least 1 MP, if it can
                    // transport the other unit, and if the other hasn't moved
                    // then enable the "Load" button.
                    if ((ce.getWalkMP() > 0) && ce.canLoad(other) && other.isLoadableThisTurn()) {
                        setLoadEnabled(true);
                        isGood = true;
                    }

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
            System.err.println("MovementDisplay#getUnloadedUnit() called without loaded units."); //$NON-NLS-1$

        }

        // If we have multiple choices, display a selection dialog.
        else if (loadedUnits.size() > 1) {
            String[] names = new String[loadedUnits.size()];
            String question = Messages.getString("MovementDisplay.UnloadUnitDialog.message", new Object[] { //$NON-NLS-1$
                    ce.getShortName(), ce.getUnusedString() });
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = loadedUnits.elementAt(loop).getShortName();
            }
            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.UnloadUnitDialog.title"), //$NON-NLS-1$
                    question, names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
                choice = loadedUnits.elementAt(choiceDialog.getChoice());
            }
        } // End have-choices

        // Only one choice.
        else {
            choice = loadedUnits.elementAt(0);
            loadedUnits.removeElementAt(0);
        }

        // Return the chosen unit.
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
            if (client.game.useVectorMove()) {
                // not where you are, but where you will be
                loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
            }
            Entity other = null;
            Enumeration<Entity> entities = client.game.getEntities(loadeePos);
            boolean isGood = false;
            while (entities.hasMoreElements()) {
                // Is the other unit friendly and not the current entity?
                other = entities.nextElement();
                if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                    // must be done with its movement
                    // it also must be same heading and velocity
                    if ((other instanceof Aero) && other.isDone() && other.canLoad(ce) && (cmd.getFinalFacing() == other.getFacing()) && !other.isCapitalFighter()) {
                        // now lets check velocity
                        // depends on movement rules
                        Aero oa = (Aero) other;
                        if (client.game.useVectorMove()) {
                            if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                                setRecoverEnabled(true);
                                isGood = true;
                            }
                        } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                            setRecoverEnabled(true);
                            isGood = true;
                        }
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
     * Joining a squadron - Similar to fighter recovery. You can fly up and join a squadron or another solo fighter
     */
    private synchronized void updateJoinButton() {

        if(!client.game.getOptions().booleanOption("stratops_capital_fighter")) {
            return;
        }

        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if(!ce.isCapitalFighter()) {
            return;
        }

        Coords loadeePos = cmd.getFinalCoords();
        if (client.game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = client.game.getEntities(loadeePos);
        boolean isGood = false;
        while (entities.hasMoreElements()) {
            // Is the other unit friendly and not the current entity?
            other = entities.nextElement();
            if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                // must be done with its movement
                // it also must be same heading and velocity
                if (other.isCapitalFighter() && other.isDone() && other.canLoad(ce) && (cmd.getFinalFacing() == other.getFacing())) {
                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (client.game.useVectorMove()) {
                        // can you do equality with vectors?
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                            setJoinEnabled(true);
                            isGood = true;
                        }
                    } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                        setJoinEnabled(true);
                        isGood = true;
                    }
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
        if ((launchableFighters.size() <= 0) && (launchableSmallCraft.size() <= 0)) {
            System.err.println("MovementDisplay#getUnloadedUnit() called without loaded units."); //$NON-NLS-1$

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
                 * We will assume that if more fighters are launched than is safe, that these excess fighters
                 * will be distributed equally among available doors
                 */
                doors = currentBay.getDoors();
                if (currentFighters.size() > 0) {
                    String[] names = new String[currentFighters.size()];
                    String question = Messages.getString("MovementDisplay.LaunchFighterDialog.message", new Object[] { //$NON-NLS-1$
                            ce.getShortName(), doors*2, bayNum});
                    for(int loop = 0; loop < names.length; loop++) {
                        names[loop] = currentFighters.elementAt(loop).getShortName();
                    }
                    ChoiceDialog choiceDialog = new ChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.LaunchFighterDialog.title", new Object[] { //$NON-NLS-1$
                            currentBay.getType(), bayNum}),
                            question, names);
                    choiceDialog.setVisible(true);
                    if (choiceDialog.getAnswer() == true) {
                        // load up the choices
                        int[] unitsLaunched = choiceDialog.getChoices();
                        for (int element : unitsLaunched) {
                                bayChoices.add(currentFighters.elementAt(element).getId());
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
     * @return the unit id that the player wants to be recovered by
     */
    private int getRecoveryUnit() {
        Entity ce = ce();
        Vector<Integer> choices = new Vector<Integer>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (client.game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = client.game.getEntities(loadeePos);
        while (entities.hasMoreElements()) {
            // Is the other unit friendly and not the current entity?
            other = entities.nextElement();
            if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                // must be done with its movement
                // it also must be same heading and velocity
                if ((other instanceof Aero) && !((Aero) other).isOutControlTotal() && other.isDone() && other.canLoad(ce) && ce.isLoadableThisTurn() && (cmd.getFinalFacing() == other.getFacing())) {

                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (client.game.useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                            choices.add(other.getId());
                        }
                    } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                        choices.add(other.getId());
                    }
                }
            }
            // Nope. Discard it.
            other = null;
        } // Check the next entity in this position.

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            if (client.game.getEntity(choices.elementAt(0)).mpUsed > 0) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RecoverSureDialog.title"), //$NON-NLS-1$
                        Messages.getString("MovementDisplay.RecoverSureDialog.message") //$NON-NLS-1$
                        )) {
                    return choices.elementAt(0);
                }
            } else {
                return choices.elementAt(0);
            }
            return -1;
        }

        String[] names = new String[choices.size()];
        for (int loop = 0; loop < names.length; loop++) {
            names[loop] = client.game.getEntity(choices.elementAt(loop)).getShortName();
        }
        String question = Messages.getString("MovementDisplay.RecoverFighterDialog.message");
        SingleChoiceDialog choiceDialog = new SingleChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.RecoverFighterDialog.title"), question, names);
        choiceDialog.setVisible(true);

        if (choiceDialog.getAnswer()) {
            // if this unit is thrusting, make sure they are aware
            if (client.game.getEntity(choices.elementAt(choiceDialog.getChoice())).mpUsed > 0) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RecoverSureDialog.title"), //$NON-NLS-1$
                        Messages.getString("MovementDisplay.RecoverSureDialog.message") //$NON-NLS-1$
                        )) {
                    return choices.elementAt(choiceDialog.getChoice());
                }
            } else {
                return choices.elementAt(choiceDialog.getChoice());
            }
        }
        return -1;
    }

    /**
     * @return the unit id that the player wants to join
     */
    private int getUnitJoined() {
        Entity ce = ce();
        Vector<Integer> choices = new Vector<Integer>();

        // collect all possible choices
        Coords loadeePos = cmd.getFinalCoords();
        if (client.game.useVectorMove()) {
            // not where you are, but where you will be
            loadeePos = Compute.getFinalPosition(ce.getPosition(), cmd.getFinalVectors());
        }
        Entity other = null;
        Enumeration<Entity> entities = client.game.getEntities(loadeePos);
        while (entities.hasMoreElements()) {
            // Is the other unit friendly and not the current entity?
            other = entities.nextElement();
            if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                // must be done with its movement
                // it also must be same heading and velocity
                if ((other instanceof Aero) && !((Aero) other).isOutControlTotal() && other.isDone() && other.canLoad(ce) && ce.isLoadableThisTurn() && (cmd.getFinalFacing() == other.getFacing())) {

                    // now lets check velocity
                    // depends on movement rules
                    Aero oa = (Aero) other;
                    if (client.game.useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                            choices.add(other.getId());
                        }
                    } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                        choices.add(other.getId());
                    }
                }
            }
            // Nope. Discard it.
            other = null;
        } // Check the next entity in this position.

        if (choices.size() < 1) {
            return -1;
        }

        if (choices.size() == 1) {
            return choices.elementAt(0);
        }

        String[] names = new String[choices.size()];
        for (int loop = 0; loop < names.length; loop++) {
            names[loop] = client.game.getEntity(choices.elementAt(loop)).getShortName();
        }
        String question = Messages.getString("MovementDisplay.JoinSquadronDialog.message");
        SingleChoiceDialog choiceDialog = new SingleChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.JoinSquadronDialog.title"), question, names);
        choiceDialog.setVisible(true);

        if (choiceDialog.getAnswer()) {
            return choices.elementAt(choiceDialog.getChoice());
        }
        return -1;
    }

    // check for out of control and adjust buttons
    private void checkOOC() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;

        if (a.isOutControlTotal()) {
            disableButtons();
            butDone.setEnabled(true);
            butNext.setEnabled(true);
            butLaunch.setEnabled(true);
        }
        return;
    }

    // check for fuel and adjust buttons
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
            butLaunch.setEnabled(true);
            updateRACButton();
            updateRecoveryButton();
            updateJoinButton();
            updateDumpButton();
        }
        return;
    }

    // check for atmosphere and adjust buttons
    private void checkAtmosphere() {
        final Entity ce = ce();
        if (null == ce) {
            return;
        }

        if (!(ce instanceof Aero)) {
            return;
        }

        Aero a = (Aero) ce;
        if (client.game.getBoard().inAtmosphere()) {
            if (a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum()) {
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
     * @param pos -
     *            the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Entity ce = ce();

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = client.game.getEntities(pos);

        // Convert the choices into a List of targets.
        Vector<Targetable> targets = new Vector<Targetable>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!ce.equals(choice)) {
                targets.addElement(choice);
            }
        }

        // Is there a building in the hex?
        Building bldg = client.game.getBoard().getBuildingAt(pos);
        if (bldg != null) {
            targets.addElement(new BuildingTarget(pos, client.game.getBoard(), false));
        }

        // Do we have a single choice?
        if (targets.size() == 1) {

            // Return that choice.
            choice = targets.elementAt(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String[] names = new String[targets.size()];
            String question = Messages.getString("MovementDisplay.ChooseTargetDialog.message", new Object[] {//$NON-NLS-1$
                    pos.getBoardNum() });
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = targets.elementAt(loop).getDisplayName();
            }
            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                    question.toString(), names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
                choice = targets.elementAt(choiceDialog.getChoice());
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    private int chooseMineToLay() {
        MineLayingDialog mld = new MineLayingDialog(clientgui.frame, ce());
        mld.setVisible(true);
        if (mld.getAnswer() == true) {
            return mld.getMine();
        }
        return -1;
    }

    private void dumpBombs() {

        if (!(ce() instanceof Aero)) {
            return;
        }
        Aero a = (Aero) ce();
        int overallMoveType = IEntityMovementType.MOVE_NONE;
        if(null != cmd) {
            overallMoveType = cmd.getLastStepMovementType();
        }

        // bring up dialog to dump bombs, then make a control roll and report
        // success or failure
        // should update mp available
        BombPayloadDialog dumpBombsDialog = new BombPayloadDialog(clientgui.frame, Messages.getString("MovementDisplay.BombDumpDialog.title"), //$NON-NLS-1$
                a.getBombChoices(), false, true);
        dumpBombsDialog.setVisible(true);
        if (dumpBombsDialog.getAnswer()) {
            /*
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
                String title = Messages.getString("MovementDisplay.DumpingBombs.title"); //$NON-NLS-1$
                String body = Messages.getString("MovementDisplay.DumpFailure.message"); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
                // failed the roll, so dump all bombs
                bombsDumped = a.getBombChoices();
            } else {
                // avoided damage
                r.choose(true);
                String title = Messages.getString("MovementDisplay.DumpingBombs.title"); //$NON-NLS-1$
                String body = Messages.getString("MovementDisplay.DumpSuccessful.message"); //$NON-NLS-1$
                clientgui.doAlertDialog(title, body);
                // addReport(r);
                 *
                 */
            //}
            /*
            // now unload the bombs
            for (int j = 0; j < bombsDumped.length; j++) {
                if (bombsDumped[j] > 0) {
                    a.removeBombs(bombsDumped[j], j);
                    // Report r = new Report(5115);
                    r.subject = ce().getId();
                    r.addDesc(ce());
                    r.add("" + bombsDumped[j] + " " + Aero.bombNames[j] + "(s)");
                    // addReport(r);
                }
            }
            // update the bomb load immediately
            a.updateBombLoad();
            client.sendUpdateEntity(ce());
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
            cmd.addStep(MovePath.STEP_YAW, true, true);
            return true;
        case (ManeuverType.MAN_HALF_ROLL):
            cmd.addStep(MovePath.STEP_ROLL, true, true);
            return true;
        case (ManeuverType.MAN_BARREL_ROLL):
            cmd.addStep(MovePath.STEP_DEC, true, true);
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
                cmd.addStep(MovePath.STEP_DEC, true, true);
                vel--;
            }
            cmd.addStep(MovePath.STEP_UP);
            return true;
        case (ManeuverType.MAN_SIDE_SLIP_LEFT):
            cmd.addStep(MovePath.STEP_LATERAL_LEFT, true, true);
            return true;
        case (ManeuverType.MAN_SIDE_SLIP_RIGHT):
            cmd.addStep(MovePath.STEP_LATERAL_RIGHT, true, true);
            return true;
        case (ManeuverType.MAN_LOOP):
            cmd.addStep(MovePath.STEP_LOOP, true, true);
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

        if (client.game.getPhase() != IGame.Phase.PHASE_MOVEMENT) {
            // ignore
            return;
        }
        // else, change turn
        endMyTurn();

        if (client.isMyTurn()) {
            // Can the player unload entities stranded on immobile transports?
            if (client.canUnloadStranded()) {
                unloadStranded();
            } else {
                beginMyTurn();
            }

        } else {
            if ((e.getPlayer() == null) && (client.game.getTurn() instanceof GameTurn.UnloadStrandedTurn)) {
                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother")); //$NON-NLS-1$
            } else {
                setStatusBarText(Messages.getString("MovementDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() && (client.game.getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
            endMyTurn();
        }
        if (client.game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
            setStatusBarText(Messages.getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$
        }
    }

    //
    // ActionListener
    //
    public synchronized void actionPerformed(ActionEvent ev) {
        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (statusBarActionPerformed(ev, client)) {
            return;
        }

        if (!client.isMyTurn()) {
            // odd...
            return;
        }

        if (ev.getSource() == butDone) {
            moveTo(cmd);
        } else if (ev.getActionCommand().equals(MOVE_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(MOVE_CANCEL)) {
            clearAllMoves();
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(MOVE_UNJAM)) {
            if ((gear == MovementDisplay.GEAR_JUMP)
                    || (gear == MovementDisplay.GEAR_CHARGE)
                    || (gear == MovementDisplay.GEAR_DFA)
                    || (cmd.getMpUsed() > ce.getWalkMP())
                    || (gear == MovementDisplay.GEAR_SWIM)
                    || (gear == MovementDisplay.GEAR_RAM)) {
                // in the wrong gear
                setUnjamEnabled(false);
            } else {
                cmd.addStep(MovePath.STEP_UNJAM_RAC);
                moveTo(cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_SEARCHLIGHT)) {
            cmd.addStep(MovePath.STEP_SEARCHLIGHT);
        } else if (ev.getActionCommand().equals(MOVE_WALK)) {
            if ((gear == MovementDisplay.GEAR_JUMP) || (gear == MovementDisplay.GEAR_SWIM)) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_LAND;
        } else if (ev.getActionCommand().equals(MOVE_JUMP)) {
            if ((ce instanceof LandAirMech) && ((LandAirMech) ce).isInMode(LandAirMech.MODE_AIRMECH)) {

                if (cmd.isFlying()) {
                    cmd.addStep(MovePath.STEP_LAND);
                    gear = MovementDisplay.GEAR_JUMP;
                } else {
                    cmd.addStep(MovePath.STEP_TAKEOFF);
                    gear = MovementDisplay.GEAR_JUMP;
                }
            } else {
                if (gear != MovementDisplay.GEAR_JUMP) {
                    clearAllMoves();
                }
                if (!cmd.isJumping()) {
                    cmd.addStep(MovePath.STEP_START_JUMP);
                }
                gear = MovementDisplay.GEAR_JUMP;
            }
        } else if (ev.getActionCommand().equals(MOVE_SWIM)) {
            if (gear != MovementDisplay.GEAR_SWIM) {
                clearAllMoves();
            }
            // dcmd.addStep(MovePath.STEP_SWIM);
            gear = MovementDisplay.GEAR_SWIM;
            ce.setMovementMode((ce instanceof BipedMech) ? IEntityMovementMode.BIPED_SWIM : IEntityMovementMode.QUAD_SWIM);
        } else if (ev.getActionCommand().equals(MOVE_TURN)) {
            gear = MovementDisplay.GEAR_TURN;
        } else if (ev.getActionCommand().equals(MOVE_BACK_UP)) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_BACKUP;
        } else if (ev.getActionCommand().equals(MOVE_CLEAR)) {
            clearAllMoves();
            if (!client.game.containsMinefield(ce.getPosition())) {
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantClearMinefield"), //$NON-NLS-1$
                        Messages.getString("MovementDisplay.NoMinefield")); //$NON-NLS-1$
                return;
            }


            int clear = Minefield.CLEAR_NUMBER_INFANTRY;
            int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;
            // Does the entity has a minesweeper?
            //TODO: not the way this is handled anymore, need to know whether they are specially trained
            /*
            for (Mounted mounted : ce.getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_TOOLS) && mounted.getType().hasSubType(MiscType.S_MINESWEEPER)) {
                    int sweeperType = mounted.getType().getToHitModifier();
                    clear = Minefield.CLEAR_NUMBER_SWEEPER[sweeperType];
                    boom = Minefield.CLEAR_NUMBER_SWEEPER_ACCIDENT[sweeperType];
                    break;
                }
            }
            */

            //need to choose a mine
            Vector<Minefield> mfs = client.game.getMinefields(ce.getPosition());
            String[] choices = new String[mfs.size()];
            for (int loop = 0; loop < choices.length; loop++) {
                choices[loop] = Minefield.getDisplayableName(mfs.elementAt(loop).getType());
            }
            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.ChooseMinefieldDialog.title"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ChooseMinefieldDialog.message"), choices);
            choiceDialog.setVisible(true);
            Minefield mf = null;
            if (choiceDialog.getAnswer() == true) {
                mf = mfs.elementAt(choiceDialog.getChoice());
            }

            if ((null != mf) && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ClearMinefieldDialog.title"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ClearMinefieldDialog.message", new Object[] { //$NON-NLS-1$
                            new Integer(clear), new Integer(boom) }))) {
                cmd.addStep(MovePath.STEP_CLEAR_MINEFIELD, mf);
                moveTo(cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_CHARGE)) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_CHARGE;
        } else if (ev.getActionCommand().equals(MOVE_DFA)) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_DFA;
            if (!cmd.isJumping()) {
                cmd.addStep(MovePath.STEP_START_JUMP);
            }
        } else if (ev.getActionCommand().equals(MOVE_RAM)) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_RAM;
        } else if (ev.getActionCommand().equals(MOVE_GET_UP)) {
            //if the unit has a hull down step
            //then don't clear the moves
            if(!cmd.contains(MovePath.STEP_HULL_DOWN)) {
                clearAllMoves();
            }

            if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                cmd.addStep(MovePath.STEP_GET_UP);
            }
            clientgui.bv.drawMovementData(ce, cmd);
        }else if (ev.getActionCommand().equals(MOVE_CAREFUL_STAND)) {
            clearAllMoves();

            ce.setCarefulStand(true);
            if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                cmd.addStep(MovePath.STEP_CAREFUL_STAND);
            }
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_GO_PRONE)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                cmd.addStep(MovePath.STEP_GO_PRONE);
            }
            clientgui.bv.drawMovementData(ce, cmd);
            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_HULL_DOWN)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalHullDown()) {
                cmd.addStep(MovePath.STEP_HULL_DOWN);
            }
            clientgui.bv.drawMovementData(ce, cmd);
            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_FLEE) && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.EscapeDialog.title"), Messages.getString("MovementDisplay.EscapeDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
            clearAllMoves();
            cmd.addStep(MovePath.STEP_FLEE);
            moveTo(cmd);
        } else if (ev.getActionCommand().equals(MOVE_EJECT)) {
            if (ce instanceof Tank) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog.title"), Messages.getString("MovementDisplay.AbandonDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clearAllMoves();
                    cmd.addStep(MovePath.STEP_EJECT);
                    moveTo(cmd);
                }
            } else if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog1.title"), Messages.getString("MovementDisplay.AbandonDialog1.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                clearAllMoves();
                cmd.addStep(MovePath.STEP_EJECT);
                moveTo(cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_LOAD)) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = null;
            Enumeration<Entity> entities = client.game.getEntities(ce.getPosition());
            while (entities.hasMoreElements()) {
                other = entities.nextElement();
                if (!ce.getOwner().isEnemyOf(other.getOwner()) && !ce.equals(other)) {
                    loadedUnits.addElement(other);
                    break;
                }
                other = null;
            }

            if (other != null) {
                cmd.addStep(MovePath.STEP_LOAD);
                clientgui.bv.drawMovementData(ce, cmd);
                gear = MovementDisplay.GEAR_LAND;
            } // else - didn't find a unit to load
        } else if (ev.getActionCommand().equals(MOVE_UNLOAD)) {
            // Ask the user if we're carrying multiple units.
            Entity other = getUnloadedUnit();

            if (other != null) {
                cmd.addStep(MovePath.STEP_UNLOAD, other);
                clientgui.bv.drawMovementData(ce, cmd);
            } // else - Player canceled the unload.
        } else if (ev.getActionCommand().equals(MOVE_RAISE_ELEVATION)) {
            cmd.addStep(MovePath.STEP_UP);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_LOWER_ELEVATION)) {
            // if this movement path goes down more than two altitudes
            // then add acceleration.
            // TODO: Is there somewhere better to put this?
            if ((ce instanceof Aero) && (null != cmd.getLastStep())
                    && (cmd.getLastStep().getNDown() == 1) && (cmd.getLastStep().getVelocity() < 12)
                    && !(((Aero) ce).isSpheroid() || client.game.getPlanetaryConditions().isVacuum())) {
                cmd.addStep(MovePath.STEP_ACC, true);
            }
            cmd.addStep(MovePath.STEP_DOWN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_CLIMB_MODE)) {
            MoveStep ms = cmd.getLastStep();
            if ((ms != null) && ((ms.getType() == MovePath.STEP_CLIMB_MODE_ON) || (ms.getType() == MovePath.STEP_CLIMB_MODE_OFF))) {
                cmd.removeLastStep();
            } else if (cmd.getFinalClimbMode()) {
                cmd.addStep(MovePath.STEP_CLIMB_MODE_OFF);
            } else {
                cmd.addStep(MovePath.STEP_CLIMB_MODE_ON);
            }
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_LAY_MINE)) {
            int i = chooseMineToLay();
            if (i != -1) {
                Mounted m = ce().getEquipment(i);
                if (m.getMineType() == Mounted.MINE_VIBRABOMB) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.frame);
                    vsd.setVisible(true);
                    m.setVibraSetting(vsd.getSetting());
                }
                cmd.addStep(MovePath.STEP_LAY_MINE, i);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_DIG_IN)) {
            cmd.addStep(MovePath.STEP_DIG_IN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_FORTIFY)) {
            cmd.addStep(MovePath.STEP_FORTIFY);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_SHAKE_OFF)) {
            cmd.addStep(MovePath.STEP_SHAKE_OFF_SWARMERS);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_MODE_MECH)) {
            clearAllMoves();
            if (ce instanceof LandAirMech) {
                final LandAirMech lam = (LandAirMech) client.getEntity(ce.getId());

                lam.convertToMode(LandAirMech.MODE_MECH);
                updateTransformationButtons(lam);
                clientgui.mechD.displayEntity(lam);
            }
        } else if (ev.getActionCommand().equals(MOVE_MODE_AIRMECH)) {
            clearAllMoves();
            if (ce instanceof LandAirMech) {
                final LandAirMech lam = (LandAirMech) client.getEntity(ce.getId());

                lam.convertToMode(LandAirMech.MODE_AIRMECH);
                updateTransformationButtons(lam);
                clientgui.mechD.displayEntity(lam);
            }
        } else if (ev.getActionCommand().equals(MOVE_MODE_AIRCRAFT)) {
            clearAllMoves();
            if (ce instanceof LandAirMech) {
                final LandAirMech lam = (LandAirMech) client.getEntity(ce.getId());

                lam.convertToMode(LandAirMech.MODE_AIRCRAFT);
                updateTransformationButtons(lam);
                clientgui.mechD.displayEntity(lam);
            }
        } else if (ev.getActionCommand().equals(MOVE_RECKLESS)) {
            cmd.setCareful(false);
        } else if (ev.getActionCommand().equals(MOVE_ACCN)) {
            cmd.addStep(MovePath.STEP_ACCN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DECN)) {
            cmd.addStep(MovePath.STEP_DECN);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_ACC)) {
            cmd.addStep(MovePath.STEP_ACC);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DEC)) {
            cmd.addStep(MovePath.STEP_DEC);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_EVADE)) {
            cmd.addStep(MovePath.STEP_EVADE);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_EVADE_AERO)) {
            cmd.addStep(MovePath.STEP_EVADE);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_ROLL)) {
            cmd.addStep(MovePath.STEP_ROLL);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_HOVER)) {
            cmd.addStep(MovePath.STEP_HOVER);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_MANEUVER)) {
            ManeuverChoiceDialog choiceDialog = new ManeuverChoiceDialog(clientgui.frame, Messages.getString("MovementDisplay.ManeuverDialog.title"), //$NON-NLS-1$
                    "huh?");
            Aero a = (Aero) ce;
            MoveStep last = cmd.getLastStep();
            int vel = a.getCurrentVelocity();
            int elev = a.getElevation();
            Coords pos = a.getPosition();
            int distance = 0;
            if (null != last) {
                vel = last.getVelocityLeft();
                elev = last.getElevation();
                pos = last.getPosition();
                distance = last.getDistance();
            }
            int ceil = client.game.getBoard().getHex(pos).ceiling();
            choiceDialog.checkPerformability(vel, elev, ceil, a.isVSTOL(), distance);
            choiceDialog.setVisible(true);
            int manType = choiceDialog.getChoice();
            if ((manType > ManeuverType.MAN_NONE) && addManeuver(manType)) {
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_LAUNCH)) {
            TreeMap<Integer, Vector<Integer>> launched = getLaunchedUnits();
            if(!launched.isEmpty()) {
                cmd.addStep(MovePath.STEP_LAUNCH, launched);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_RECOVER)) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int recoverer = getRecoveryUnit();
            if (recoverer != -1) {
                cmd.addStep(MovePath.STEP_RECOVER, recoverer, -1);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_JOIN)) {
            // if more than one unit is available as a carrier
            // then bring up an option dialog
            int joined = getUnitJoined();
            if (joined != -1) {
                cmd.addStep(MovePath.STEP_JOIN, joined, -1);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_TURN_LEFT)) {
            cmd.addStep(MovePath.STEP_TURN_LEFT);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_TURN_RIGHT)) {
            cmd.addStep(MovePath.STEP_TURN_RIGHT);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_THRUST)) {
            cmd.addStep(MovePath.STEP_THRUST);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_YAW)) {
            cmd.addStep(MovePath.STEP_YAW);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_END_OVER)) {
            cmd.addStep(MovePath.STEP_YAW);
            cmd.addStep(MovePath.STEP_ROLL);
            clientgui.bv.drawMovementData(ce, cmd);
        } else if (ev.getActionCommand().equals(MOVE_DUMP)) {
            dumpBombs();
        }

        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateLoadButtons();
        updateElevationButtons();
        updateFleeButton();
        updateLaunchButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();
        updateDumpButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();
    }

    /**
     * Give the player the opportunity to unload all entities that are stranded
     * on immobile transports. <p/> According to <a
     * href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">
     * Randall Bills</a>, the "minimum move" rule allow stranded units to
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
        Enumeration<Entity> entities = client.getSelectedEntities(new EntitySelector() {
            private final IGame game = client.game;
            private final GameTurn turn = client.game.getTurn();
            private final int ownerId = client.getLocalPlayer().getId();

            public boolean accept(Entity entity) {
                if (turn.isValid(ownerId, entity, game)) {
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
            transport = client.getEntity(entity.getTransportId());
            String buffer;
            if (null == transport) {
                buffer = entity.getDisplayName();
            } else {
                buffer = Messages.getString("MovementDisplay.EntityAt", new Object[] { entity.getDisplayName(), transport.getPosition().getBoardNum() }); //$NON-NLS-1$
            }
            names[index] = buffer.toString();
        }

        // Show the choices to the player

        int[] indexes = clientgui.doChoiceDialog(Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.title"), //$NON-NLS-1$
                Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.message"), //$NON-NLS-1$
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
        client.sendUnloadStranded(ids);
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearAllMoves();
        }
        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (client.isMyTurn()) {
                removeLastStep();
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_ENTER) && ev.isControlDown()) {
            if (client.isMyTurn()) {
                moveTo(cmd);
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && (clientgui.getBoardView().getLastCursor() != null) && !clientgui.getBoardView().getLastCursor().equals(clientgui.getBoardView().getSelected())) {
                // switch to turning
                // clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
                clientgui.bv.drawMovementData(ce, cmd);
            }
        }

        // arrow can also rotate when shift is down
        if (shiftheld && client.isMyTurn() && ((ev.getKeyCode() == KeyEvent.VK_LEFT) || (ev.getKeyCode() == KeyEvent.VK_RIGHT))) {
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
            clientgui.bv.drawMovementData(ce, cmd);
        }
    }

    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && shiftheld) {
            shiftheld = false;
            if (client.isMyTurn() && (clientgui.getBoardView().getLastCursor() != null) && !clientgui.getBoardView().getLastCursor().equals(clientgui.getBoardView().getSelected())) {
                // switch to movement
                clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
                clientgui.bv.drawMovementData(ce(), cmd);
            }
        }
    }

    public void keyTyped(KeyEvent ev) {
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        final Entity ce = ce();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() && (ce != null)) {
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

        Entity e = client.game.getEntity(b.getEntityId());
        if (null == e) {
            return;
        }
        if (client.isMyTurn()) {
            if (client.game.getTurn().isValidEntity(e, client.game)) {
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

    private void setLayMineEnabled(boolean enabled) {
        butLayMine.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLayMineEnabled(enabled);
    }

    private void setLoadEnabled(boolean enabled) {
        butLoad.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLoadEnabled(enabled);
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
            butSearchlight.setLabel(Messages.getString("MovementDisplay.butSearchlightOff")); //$NON-NLS-1$
        } else {
            butSearchlight.setLabel(Messages.getString("MovementDisplay.butSearchlightOn")); //$NON-NLS-1$
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

    private void setLAMmechModeEnabled(boolean enabled) {
        clientgui.getMenuBar().setMoveLAMmechModeEnabled(enabled);
    }

    private void setLAMairmechModeEnabled(boolean enabled) {
        clientgui.getMenuBar().setMoveLAMairmechModeEnabled(enabled);
    }

    private void setLAMaircraftModeEnabled(boolean enabled) {
        clientgui.getMenuBar().setMoveLAMaircraftModeEnabled(enabled);
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
     * Retrieve the "Done" button of this object.
     *
     * @return the <code>java.awt.Button</code> that activates this object's
     *         "Done" action.
     */
    public Button getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

    /*
     * Add steps for advanced vector movement based on the given vectors when
     * splitting hexes, choose the hex with less tonnage in case OOC
     */
    private MovePath addSteps(MovePath md, Entity en) {

        // if the last step is a launch or recovery, then I want to keep that at
        // the end
        MoveStep lastStep = md.getLastStep();
        if ((lastStep != null) && ((lastStep.getType() == MovePath.STEP_LAUNCH) || (lastStep.getType() == MovePath.STEP_RECOVER))) {
            md.removeLastStep();
        }

        // get the start and end
        Coords start = en.getPosition();
        Coords end = Compute.getFinalPosition(start, md.getFinalVectors());
        // Coords end = md.getFinalVectors().getFinalPosition(start);

        // (see LosEffects.java)
        ArrayList<Coords> in = Coords.intervening(start, end);
        // first check whether we are splitting hexes
        boolean split = false;
        double degree = start.degree(end);
        if (degree % 60 == 30) {
            split = true;
            in = Coords.intervening(start, end, true);
        }

        Coords current = start;
        int facing = md.getFinalFacing();
        for (int i = 1; i < in.size(); i++) {

            // check for split hexes
            // check for some number after a multiple of 3 (1,4,7,etc)
            if (((i % 3) == 1) && split) {

                Coords left = in.get(i);
                Coords right = in.get(i + 1);

                // get the total tonnage in each hex
                Enumeration<Entity> leftTargets = client.game.getEntities(left);
                double leftTonnage = 0;
                while (leftTargets.hasMoreElements()) {
                    leftTonnage += leftTargets.nextElement().getWeight();
                }
                Enumeration<Entity> rightTargets = client.game.getEntities(right);
                double rightTonnage = 0;
                while (rightTargets.hasMoreElements()) {
                    rightTonnage += rightTargets.nextElement().getWeight();
                }

                // TODO: I will need to update this to account for asteroids

                // I need to consider both of these passed through
                // for purposes of bombing
                en.addPassedThrough(right);
                en.addPassedThrough(left);
                client.sendUpdateEntity(en);

                // if the left is preferred, increment i so next one is skipped
                if (leftTonnage < rightTonnage) {
                    i++;
                } else {
                    continue;
                }

            }

            Coords c = in.get(i);

            // check if the next move would put vessel off the map
            if (!client.game.getBoard().contains(c)) {
                md.addStep(MovePath.STEP_OFF);
                break;
            }

            // which direction is this from the current hex?
            int dir = current.direction(c);
            // what kind of step do I need to get there?
            int diff = dir - facing;
            if (diff == 0) {
                md.addStep(MovePath.STEP_FORWARDS);
            } else if ((diff == 1) || (diff == -5)) {
                md.addStep(MovePath.STEP_LATERAL_RIGHT);
            } else if ((diff == -2) || (diff == 4)) {
                md.addStep(MovePath.STEP_LATERAL_RIGHT_BACKWARDS);
            } else if ((diff == -1) || (diff == 5)) {
                md.addStep(MovePath.STEP_LATERAL_LEFT);
            } else if ((diff == 2) || (diff == -4)) {
                md.addStep(MovePath.STEP_LATERAL_LEFT_BACKWARDS);
            } else if ((diff == 3) || (diff == -3)) {
                md.addStep(MovePath.STEP_BACKWARDS);
            }

            current = c;

        }

        // do I now need to add on the last step again?
        if ((lastStep != null) && (lastStep.getType() == MovePath.STEP_LAUNCH)) {
            md.addStep(MovePath.STEP_LAUNCH, lastStep.getLaunched());
        }

        if ((lastStep != null) && (lastStep.getType() == MovePath.STEP_RECOVER)) {
            md.addStep(MovePath.STEP_RECOVER, lastStep.getRecoveryUnit());
        }

        return md;
    }

}
