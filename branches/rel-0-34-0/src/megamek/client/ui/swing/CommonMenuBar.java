/*
 * MegaMek - Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.MovementDisplay;
import megamek.common.Entity;
import megamek.common.IGame;

/**
 * Every menu bar in MegaMek should have an identical look-and-feel, with
 * various menu items enabled or disabled, based upon the frame that owns the
 * menu bar, and the current state of the program.
 */
public class CommonMenuBar extends JMenuBar implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -3696211330827384307L;
    /**
     * The <code>Game</code> current selected. This value may be
     * <code>null</code>.
     */
    private IGame game;
    private JMenuItem fileGameNew;
    private JMenuItem fileGameOpen;
    private JMenuItem fileGameSave;
    private JMenuItem fileGameScenario;
    private JMenuItem fileGameConnectBot;
    private JMenuItem fileGameConnect;
    /**
     * When we have a <code>Board</code>, set this to <code>true</code>.
     */
    private boolean hasBoard;
    private JMenuItem fileBoardNew;
    private JMenuItem fileBoardOpen;
    private JMenuItem fileBoardSave;
    private JMenuItem fileBoardSaveAs;
    private JMenuItem fileBoardSaveAsImage;
    /**
     * When we have a unit list, set this to <code>true</code>.
     */
    private boolean hasUnitList;
    private JMenuItem fileUnitsOpen;
    private JMenuItem fileUnitsClear;
    private JMenuItem fileUnitsSave;
    /**
     * The <code>Entity</code> current selected. This value may be
     * <code>null</code>.
     */
    private Entity entity;
    /**
     * Record the current phase of the game.
     */
    private IGame.Phase phase = IGame.Phase.PHASE_UNKNOWN;
    private JMenuItem filePrint;
    private JMenuItem viewMiniMap;
    private JMenuItem viewMekDisplay;
    private JMenuItem viewZoomIn;
    private JMenuItem viewZoomOut;
    private JMenuItem viewLOSSetting;
    private JMenuItem viewUnitOverview;
    private JMenuItem viewRoundReport;
    private JMenuItem viewGameOptions;
    private JMenuItem viewClientSettings;
    private JMenuItem viewPlayerList;
    private JMenuItem deployMinesConventional;
    private JMenuItem deployMinesCommand;
    private JMenuItem deployMinesVibrabomb;
    private JMenuItem deployMinesActive;
    private JMenuItem deployMinesInferno;
    private JMenuItem deployNext;
    private JMenuItem deployTurn;
    private JMenuItem deployLoad;
    private JMenuItem deployUnload;
    private JMenuItem deployRemove;
    private JMenuItem deployAssaultDrop;
    private JMenuItem moveWalk;
    private JMenuItem moveNext;
    private JMenuItem moveTurn;
    private JMenuItem moveLayMine;
    private JMenuItem moveLoad;
    private JMenuItem moveUnload;
    private JMenuItem moveJump;
    private JMenuItem moveSwim;
    private JMenuItem moveBackUp;
    private JMenuItem moveCharge;
    private JMenuItem moveDFA;
    private JMenuItem moveGoProne;
    private JMenuItem moveFlee;
    private JMenuItem moveEject;
    private JMenuItem moveUnjam;
    private JMenuItem moveSearchlight;
    private JMenuItem moveClear;
    private JMenuItem moveHullDown;
    private JMenuItem moveGetUp;
    private JMenuItem moveRaise;
    private JMenuItem moveLower;
    private JMenuItem moveReckless;
    private JMenuItem moveEvade;
    private JMenuItem moveAcc = null;
    private JMenuItem moveDec = null;
    private JMenuItem moveAccN = null;
    private JMenuItem moveDecN = null;
    private JMenuItem moveEvadeAero = null;
    private JMenuItem moveRoll = null;
    private JMenuItem moveLaunch = null;
    private JMenuItem moveRecover = null;
    private JMenuItem moveJoin = null;
    private JMenuItem moveDump = null;
    private JMenuItem moveRam = null;
    private JMenuItem moveHover = null;
    private JMenuItem moveManeuver = null;
    private JMenuItem moveTurnLeft = null;
    private JMenuItem moveTurnRight = null;
    private JMenuItem moveThrust = null;
    private JMenuItem moveYaw = null;
    private JMenuItem moveEndOver= null;
    private JMenuItem fireFire;
    private JMenuItem fireSkip;
    private JMenuItem fireNextTarg;
    private JMenuItem fireNext;
    private JMenuItem fireTwist;
    private JMenuItem fireFlipArms;
    private JMenuItem fireMode;
    private JMenuItem fireFindClub;
    private JMenuItem fireSpot;
    private JMenuItem fireSearchlight;
    private JMenuItem fireClearTurret;
    private JMenuItem fireClearWeaponJam;
    private JMenuItem fireCancel;
    private JMenuItem physicalNext;
    private JMenuItem physicalPunch;
    private JMenuItem physicalKick;
    private JMenuItem physicalPush;
    private JMenuItem physicalClub;
    private JMenuItem physicalBrushOff;
    private JMenuItem physicalDodge;
    private JMenuItem physicalThrash;
    private JMenuItem physicalProto;
    private Client client;
    /**
     * A <code>Vector</code> containing the <code>ActionListener</code>s
     * that have registered themselves with this menu bar.
     */
    private final Vector<ActionListener> actionListeners = new Vector<ActionListener>();

    /**
     * Create a MegaMek menu bar.
     */
    public CommonMenuBar(Client parent) {
        this();
        client = parent;
    }

    public CommonMenuBar() {
        JMenu menu = null;
        JMenu submenu = null;
        JMenu aeromenu = null;
        JMenuItem item = null;

        // *** Create the File menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.FileMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_F);
        add(menu);

        // Create the Game sub-menu.
        submenu = new JMenu(Messages.getString("CommonMenuBar.GameMenu")); //$NON-NLS-1$
        menu.add(submenu);
        fileGameNew = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameNew")); //$NON-NLS-1$
        fileGameNew.addActionListener(this);
        fileGameNew.setActionCommand("fileGameNew"); //$NON-NLS-1$
        fileGameNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                getToolkit().getMenuShortcutKeyMask()));
        submenu.add(fileGameNew);
        fileGameOpen = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameOpen")); //$NON-NLS-1$
        fileGameOpen.addActionListener(this);
        fileGameOpen.setActionCommand("fileGameOpen"); //$NON-NLS-1$
        submenu.add(fileGameOpen);
        fileGameSave = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameSave")); //$NON-NLS-1$
        fileGameSave.addActionListener(this);
        fileGameSave.setActionCommand("fileGameSave"); //$NON-NLS-1$
        submenu.add(fileGameSave);
        submenu.addSeparator();
        fileGameScenario = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameScenario")); //$NON-NLS-1$
        fileGameScenario.addActionListener(this);
        fileGameScenario.setActionCommand("fileGameScenario"); //$NON-NLS-1$
        submenu.add(fileGameScenario);
        submenu.addSeparator();
        fileGameConnectBot = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameConnectBot")); //$NON-NLS-1$
        fileGameConnectBot.addActionListener(this);
        fileGameConnectBot.setActionCommand("fileGameConnectBot"); //$NON-NLS-1$
        submenu.add(fileGameConnectBot);
        fileGameConnect = new JMenuItem(Messages
                .getString("CommonMenuBar.fileGameConnect")); //$NON-NLS-1$
        fileGameConnect.addActionListener(this);
        fileGameConnect.setActionCommand("fileGameConnect"); //$NON-NLS-1$
        submenu.add(fileGameConnect);

        // Create the Board sub-menu.
        submenu = new JMenu(Messages.getString("CommonMenuBar.BoardMenu")); //$NON-NLS-1$
        menu.add(submenu);
        fileBoardNew = new JMenuItem(Messages
                .getString("CommonMenuBar.fileBoardNew")); //$NON-NLS-1$
        fileBoardNew.addActionListener(this);
        fileBoardNew.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        submenu.add(fileBoardNew);
        fileBoardOpen = new JMenuItem(Messages
                .getString("CommonMenuBar.fileBoardOpen")); //$NON-NLS-1$
        fileBoardOpen.addActionListener(this);
        fileBoardOpen.setActionCommand("fileBoardOpen"); //$NON-NLS-1$
        submenu.add(fileBoardOpen);
        fileBoardSave = new JMenuItem(Messages
                .getString("CommonMenuBar.fileBoardSave")); //$NON-NLS-1$
        fileBoardSave.addActionListener(this);
        fileBoardSave.setActionCommand("fileBoardSave"); //$NON-NLS-1$
        submenu.add(fileBoardSave);
        fileBoardSaveAs = new JMenuItem(Messages
                .getString("CommonMenuBar.fileBoardSaveAs")); //$NON-NLS-1$
        fileBoardSaveAs.addActionListener(this);
        fileBoardSaveAs.setActionCommand("fileBoardSaveAs"); //$NON-NLS-1$
        submenu.add(fileBoardSaveAs);
        fileBoardSaveAsImage = new JMenuItem(Messages
                .getString("CommonMenuBar.fileBoardSaveAsImage")); //$NON-NLS-1$
        fileBoardSaveAsImage.addActionListener(this);
        fileBoardSaveAsImage.setActionCommand("fileBoardSaveAsImage"); //$NON-NLS-1$
        submenu.add(fileBoardSaveAsImage);

        // Create the Unit List sub-menu.
        submenu = new JMenu(Messages.getString("CommonMenuBar.UnitListMenu")); //$NON-NLS-1$
        menu.add(submenu);
        fileUnitsOpen = new JMenuItem(Messages
                .getString("CommonMenuBar.fileUnitsOpen")); //$NON-NLS-1$
        fileUnitsOpen.addActionListener(this);
        fileUnitsOpen.setActionCommand("fileUnitsOpen"); //$NON-NLS-1$
        submenu.add(fileUnitsOpen);
        fileUnitsClear = new JMenuItem(Messages
                .getString("CommonMenuBar.fileUnitsClear")); //$NON-NLS-1$
        fileUnitsClear.addActionListener(this);
        fileUnitsClear.setActionCommand("fileUnitsClear"); //$NON-NLS-1$
        submenu.add(fileUnitsClear);
        fileUnitsSave = new JMenuItem(Messages
                .getString("CommonMenuBar.fileUnitsSave")); //$NON-NLS-1$
        fileUnitsSave.addActionListener(this);
        fileUnitsSave.setActionCommand("fileUnitsSave"); //$NON-NLS-1$
        submenu.add(fileUnitsSave);

        // Finish off the File menu.
        filePrint = new JMenuItem(Messages.getString("CommonMenuBar.PrintMenu")); //$NON-NLS-1$
        filePrint.addActionListener(this);
        filePrint.setActionCommand("filePrint"); //$NON-NLS-1$
        filePrint.setEnabled(false);
        menu.addSeparator();
        menu.add(filePrint);

        // *** Create the view menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.ViewMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_V);
        add(menu);
        viewMekDisplay = new JMenuItem(Messages
                .getString("CommonMenuBar.viewMekDisplay")); //$NON-NLS-1$
        viewMekDisplay.addActionListener(this);
        viewMekDisplay.setActionCommand(ClientGUI.VIEW_MEK_DISPLAY);
        viewMekDisplay.setMnemonic(KeyEvent.VK_D);
        viewMekDisplay.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                getToolkit().getMenuShortcutKeyMask()));
        menu.add(viewMekDisplay);
        viewMiniMap = new JMenuItem(Messages
                .getString("CommonMenuBar.viewMiniMap")); //$NON-NLS-1$
        viewMiniMap.addActionListener(this);
        viewMiniMap.setActionCommand(ClientGUI.VIEW_MINI_MAP);
        viewMiniMap.setMnemonic(KeyEvent.VK_M);
        viewMiniMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
                getToolkit().getMenuShortcutKeyMask()));
        menu.add(viewMiniMap);
        viewUnitOverview = new JMenuItem(Messages
                .getString("CommonMenuBar.viewUnitOverview")); //$NON-NLS-1$
        viewUnitOverview.addActionListener(this);
        viewUnitOverview.setActionCommand(ClientGUI.VIEW_UNIT_OVERVIEW);
        viewUnitOverview.setMnemonic(KeyEvent.VK_U);
        viewUnitOverview.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                getToolkit().getMenuShortcutKeyMask()));
        menu.add(viewUnitOverview);
        viewZoomIn = new JMenuItem(Messages
                .getString("CommonMenuBar.viewZoomIn")); //$NON-NLS-1$
        viewZoomIn.addActionListener(this);
        viewZoomIn.setActionCommand(ClientGUI.VIEW_ZOOM_IN);
        menu.add(viewZoomIn);
        viewZoomOut = new JMenuItem(Messages
                .getString("CommonMenuBar.viewZoomOut")); //$NON-NLS-1$
        viewZoomOut.addActionListener(this);
        viewZoomOut.setActionCommand(ClientGUI.VIEW_ZOOM_OUT);
        menu.add(viewZoomOut);
        menu.addSeparator();
        viewRoundReport = new JMenuItem(Messages
                .getString("CommonMenuBar.viewRoundReport")); //$NON-NLS-1$
        viewRoundReport.addActionListener(this);
        viewRoundReport.setActionCommand("viewRoundReport"); //$NON-NLS-1$
        viewRoundReport.setMnemonic(KeyEvent.VK_R);
        viewRoundReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                getToolkit().getMenuShortcutKeyMask()));
        menu.add(viewRoundReport);
        menu.addSeparator();
        viewGameOptions = new JMenuItem(Messages
                .getString("CommonMenuBar.viewGameOptions")); //$NON-NLS-1$
        viewGameOptions.setActionCommand("viewGameOptions"); //$NON-NLS-1$
        viewGameOptions.addActionListener(this);
        menu.add(viewGameOptions);
        viewClientSettings = new JMenuItem(Messages
                .getString("CommonMenuBar.viewClientSettings")); //$NON-NLS-1$
        viewClientSettings.setActionCommand("viewClientSettings"); //$NON-NLS-1$
        viewClientSettings.addActionListener(this);
        menu.add(viewClientSettings);
        viewLOSSetting = new JMenuItem(Messages
                .getString("CommonMenuBar.viewLOSSetting")); //$NON-NLS-1$
        viewLOSSetting.addActionListener(this);
        viewLOSSetting.setActionCommand(ClientGUI.VIEW_LOS_SETTING);
        viewLOSSetting.setMnemonic(KeyEvent.VK_L);
        viewLOSSetting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                getToolkit().getMenuShortcutKeyMask()));
        menu.add(viewLOSSetting);
        menu.addSeparator();
        viewPlayerList = new JMenuItem(Messages
                .getString("CommonMenuBar.viewPlayerList")); //$NON-NLS-1$
        viewPlayerList.setActionCommand("viewPlayerList"); //$NON-NLS-1$
        viewPlayerList.addActionListener(this);
        menu.add(viewPlayerList);

        // *** Create the deployo menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.DeployMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_D);
        add(menu);

        // Create the Mines sub-menu.
        submenu = new JMenu(Messages.getString("CommonMenuBar.DeployMinesMenu")); //$NON-NLS-1$
        deployMinesConventional = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.deployMinesConventional"), DeployMinefieldDisplay.DEPLOY_MINE_CONV); //$NON-NLS-1$
        deployMinesCommand = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.deployMinesCommand"), DeployMinefieldDisplay.DEPLOY_MINE_COM); //$NON-NLS-1$
        deployMinesVibrabomb = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.deployMinesVibrabomb"), DeployMinefieldDisplay.DEPLOY_MINE_VIBRA); //$NON-NLS-1$
        deployMinesActive = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.deployMinesActive"), DeployMinefieldDisplay.DEPLOY_MINE_ACTIVE); //$NON-NLS-1$
        deployMinesInferno = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.deployMinesInferno"), DeployMinefieldDisplay.DEPLOY_MINE_INFERNO); //$NON-NLS-1$

        // Finish off the deploy menu.
        deployNext = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployNext"), DeploymentDisplay.DEPLOY_NEXT, KeyEvent.VK_N); //$NON-NLS-1$
        deployTurn = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployTurn"), DeploymentDisplay.DEPLOY_TURN); //$NON-NLS-1$
        deployLoad = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployLoad"), DeploymentDisplay.DEPLOY_LOAD); //$NON-NLS-1$
        deployUnload = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployUnload"), DeploymentDisplay.DEPLOY_UNLOAD); //$NON-NLS-1$
        deployRemove = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployRemove"), DeploymentDisplay.DEPLOY_REMOVE); //$NON-NLS-1$
        deployAssaultDrop = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.deployAssaultDrop"), DeploymentDisplay.DEPLOY_ASSAULTDROP); //$NON-NLS-1$
        menu.addSeparator();
        menu.add(submenu);
        
        // *** Create the move menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.MoveMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_M);
        add(menu);
        moveWalk = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveWalk"), MovementDisplay.MOVE_WALK, KeyEvent.VK_W); //$NON-NLS-1$
        moveJump = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveJump"), MovementDisplay.MOVE_JUMP, KeyEvent.VK_J); //$NON-NLS-1$
        moveSwim = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveSwim"), MovementDisplay.MOVE_SWIM, KeyEvent.VK_S); //$NON-NLS-1$
        moveBackUp = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveBackUp"), MovementDisplay.MOVE_BACK_UP); //$NON-NLS-1$
        moveGetUp = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveGetUp"), MovementDisplay.MOVE_GET_UP); //$NON-NLS-1$
        moveGoProne = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveGoProne"), MovementDisplay.MOVE_GO_PRONE); //$NON-NLS-1$
        moveTurn = createMenuItem(menu, Messages
                .getString("CommonMenuBar.moveTurn"), MovementDisplay.MOVE_TURN); //$NON-NLS-1$
        moveNext = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveNext"), MovementDisplay.MOVE_NEXT, KeyEvent.VK_N); //$NON-NLS-1$
        moveRaise = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveRaise"), MovementDisplay.MOVE_RAISE_ELEVATION); //$NON-NLS-1$
        moveLower = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveLower"), MovementDisplay.MOVE_LOWER_ELEVATION); //$NON-NLS-1$
        moveReckless = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveReckless"), MovementDisplay.MOVE_RECKLESS); //$NON-NLS-1$
        moveEvade = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveEvade"), MovementDisplay.MOVE_EVADE); //$NON-NLS-1$
        
        //create aero movement sub-menu
        aeromenu = new JMenu(Messages.getString("CommonMenuBar.AeroMenu")); //$NON-NLS-1$
        moveAcc = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveAcc"), MovementDisplay.MOVE_ACC); //$NON-NLS-1$
        moveDec = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveDec"), MovementDisplay.MOVE_DEC); //$NON-NLS-1$
        moveAccN = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveAccN"), MovementDisplay.MOVE_ACCN); //$NON-NLS-1$
        moveDecN = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveDecN"), MovementDisplay.MOVE_DECN); //$NON-NLS-1$
        moveEvadeAero = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveEvadeAero"), MovementDisplay.MOVE_EVADE_AERO); //$NON-NLS-1$
        moveRoll = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveRoll"), MovementDisplay.MOVE_ROLL); //$NON-NLS-1$
        aeromenu.addSeparator();
        moveHover = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveHover"), MovementDisplay.MOVE_HOVER); //$NON-NLS-1$
        moveManeuver = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveManeuver"), MovementDisplay.MOVE_MANEUVER); //$NON-NLS-1$
        moveTurnLeft = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveTurnLeft"), MovementDisplay.MOVE_TURN_LEFT); //$NON-NLS-1$
        moveTurnRight = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveTurnRight"), MovementDisplay.MOVE_TURN_RIGHT); //$NON-NLS-1$
        moveThrust = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveThrust"), MovementDisplay.MOVE_THRUST); //$NON-NLS-1$
        moveYaw = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveYaw"), MovementDisplay.MOVE_YAW); //$NON-NLS-1$
        moveEndOver = createMenuItem(aeromenu, Messages.getString("CommonMenuBar.moveEndOver"), MovementDisplay.MOVE_END_OVER); //$NON-NLS-1$
        
        menu.addSeparator();
        menu.add(aeromenu);
        
        // Create the Special sub-menu.
        submenu = new JMenu(Messages.getString("CommonMenuBar.SpecialMenu")); //$NON-NLS-1$
        moveLoad = createMenuItem(submenu, Messages
                .getString("CommonMenuBar.MoveLoad"), MovementDisplay.MOVE_LOAD); //$NON-NLS-1$
        moveUnload = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.MoveUnload"), MovementDisplay.MOVE_UNLOAD); //$NON-NLS-1$
        moveLaunch = createMenuItem(submenu, Messages.getString("CommonMenuBar.moveLaunch"), MovementDisplay.MOVE_LAUNCH); //$NON-NLS-1$
        moveRecover = createMenuItem(submenu, Messages.getString("CommonMenuBar.moveRecover"), MovementDisplay.MOVE_RECOVER); //$NON-NLS-1$
        moveJoin = createMenuItem(submenu, Messages.getString("CommonMenuBar.moveJoin"), MovementDisplay.MOVE_JOIN); //$NON-NLS-1$
        submenu.addSeparator();
        moveCharge = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.MoveCharge"), MovementDisplay.MOVE_CHARGE); //$NON-NLS-1$
        moveDFA = createMenuItem(submenu, Messages
                .getString("CommonMenuBar.MoveDeth"), MovementDisplay.MOVE_DFA); //$NON-NLS-1$
        moveRam = createMenuItem(submenu, Messages.getString("CommonMenuBar.moveRam"), MovementDisplay.MOVE_RAM); //$NON-NLS-1$
        submenu.addSeparator();
        moveFlee = createMenuItem(submenu, Messages
                .getString("CommonMenuBar.MoveFlee"), MovementDisplay.MOVE_FLEE); //$NON-NLS-1$
        moveEject = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.MoveEject"), MovementDisplay.MOVE_EJECT); //$NON-NLS-1$
        submenu.addSeparator();
        moveUnjam = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.moveUnjam"), MovementDisplay.MOVE_UNJAM); //$NON-NLS-1$
        moveSearchlight = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.moveSearchlight"), MovementDisplay.MOVE_SEARCHLIGHT); //$NON-NLS-1$
        moveClear = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.moveClear"), MovementDisplay.MOVE_CLEAR); //$NON-NLS-1$
        moveHullDown = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.moveHullDown"), MovementDisplay.MOVE_CLEAR); //$NON-NLS-1$
        moveLayMine = createMenuItem(
                submenu,
                Messages.getString("CommonMenuBar.moveLayMine"), MovementDisplay.MOVE_LAY_MINE); //$NON-NLS-1$
        moveDump = createMenuItem(submenu, Messages.getString("CommonMenuBar.moveDump"), MovementDisplay.MOVE_DUMP); //$NON-NLS-1$ 
        
        menu.addSeparator();
        menu.add(submenu);

        // Add the cancel button.
        menu.addSeparator();
        moveNext = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.moveCancel"), MovementDisplay.MOVE_CANCEL, KeyEvent.VK_ESCAPE); //$NON-NLS-1$

        // *** Create the fire menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.FireMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_I);
        add(menu);
        fireFire = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireFire"), FiringDisplay.FIRE_FIRE, KeyEvent.VK_F); //$NON-NLS-1$
        fireSkip = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireSkip"), FiringDisplay.FIRE_SKIP, KeyEvent.VK_S); //$NON-NLS-1$
        fireNextTarg = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireNextTarg"), FiringDisplay.FIRE_NEXT_TARG, KeyEvent.VK_T); //$NON-NLS-1$
        fireNext = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireNext"), FiringDisplay.FIRE_NEXT, KeyEvent.VK_N); //$NON-NLS-1$
        menu.addSeparator();
        fireTwist = createMenuItem(menu, Messages
                .getString("CommonMenuBar.fireTwist"), FiringDisplay.FIRE_TWIST); //$NON-NLS-1$
        fireFlipArms = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireFlipArms"), FiringDisplay.FIRE_FLIP_ARMS); //$NON-NLS-1$
        menu.addSeparator();
        fireMode = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireMode"), FiringDisplay.FIRE_MODE, KeyEvent.VK_O); //$NON-NLS-1$
        menu.addSeparator();
        fireFindClub = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireFindClub"), FiringDisplay.FIRE_FIND_CLUB); //$NON-NLS-1$
        fireSpot = createMenuItem(menu, Messages
                .getString("CommonMenuBar.fireSpot"), FiringDisplay.FIRE_SPOT); //$NON-NLS-1$
        fireSearchlight = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireSearchlight"), FiringDisplay.FIRE_SEARCHLIGHT); //$NON-NLS-1$
        fireClearTurret = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireClearTurret"), FiringDisplay.FIRE_CLEAR_TURRET); //$NON-NLS-1$
        fireClearWeaponJam = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireClearWeaponJam"), FiringDisplay.FIRE_CLEAR_WEAPON); //$NON-NLS-1$
        menu.addSeparator();
        fireCancel = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.fireCancel"), FiringDisplay.FIRE_CANCEL, KeyEvent.VK_ESCAPE); //$NON-NLS-1$

        // *** Create the physical menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.PhysicalMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_P);
        add(menu);
        physicalPunch = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalPunch"), PhysicalDisplay.PHYSICAL_PUNCH); //$NON-NLS-1$
        physicalKick = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalKick"), PhysicalDisplay.PHYSICAL_KICK); //$NON-NLS-1$
        physicalPush = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalPush"), PhysicalDisplay.PHYSICAL_PUSH); //$NON-NLS-1$
        physicalClub = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalClub"), PhysicalDisplay.PHYSICAL_CLUB); //$NON-NLS-1$
        physicalBrushOff = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalBrushOff"), PhysicalDisplay.PHYSICAL_BRUSH_OFF); //$NON-NLS-1$
        physicalThrash = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalThrash"), PhysicalDisplay.PHYSICAL_THRASH); //$NON-NLS-1$
        physicalProto = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalProto"), PhysicalDisplay.PHYSICAL_PROTO); //$NON-NLS-1$
        physicalDodge = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalDodge"), PhysicalDisplay.PHYSICAL_DODGE); //$NON-NLS-1$
        physicalNext = createMenuItem(
                menu,
                Messages.getString("CommonMenuBar.physicalNext"), PhysicalDisplay.PHYSICAL_NEXT, KeyEvent.VK_N); //$NON-NLS-1$

        // *** Create the help menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.HelpMenu")); //$NON-NLS-1$
        menu.setMnemonic(KeyEvent.VK_H);
        add(menu);
        item = new JMenuItem(Messages.getString("CommonMenuBar.helpContents")); //$NON-NLS-1$
        item.addActionListener(this);
        item.setActionCommand("helpContents"); //$NON-NLS-1$
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(Messages.getString("CommonMenuBar.helpAbout")); //$NON-NLS-1$
        item.setActionCommand("helpAbout"); //$NON-NLS-1$
        item.addActionListener(this);
        menu.add(item);

        // Now manage the menu items.
        manageMenu();
    }

    private JMenuItem createMenuItem(JMenu m, String label, String command,
            int shortcut) {
        JMenuItem mi = createMenuItem(m, label, command);
        mi.setMnemonic(shortcut);
        return mi;
    }

    private JMenuItem createMenuItem(JMenu m, String label, String command) {
        JMenuItem mi = new JMenuItem(label);
        mi.addActionListener(this);
        mi.setActionCommand(command);
        mi.setEnabled(false);
        m.add(mi);
        return mi;
    }

    /**
     * Implement the <code>ActionListener</code> interface.
     * 
     * @param event - the <code>ActionEvent</code> that spawned this call.
     */
    public void actionPerformed(ActionEvent event) {
        // Pass the action on to each of our listeners.
        Enumeration<ActionListener> iter = actionListeners.elements();
        while (iter.hasMoreElements()) {
            ActionListener listener = iter.nextElement();
            listener.actionPerformed(event);
        }
    }

    /**
     * Register an object that wishes to be alerted when an item on this menu
     * bar has been selected. <p/> Please note, the ActionCommand property of
     * the action event will inform the listener as to which menu item that has
     * been selected. Not all listeners will be interested in all menu items.
     * 
     * @param listener - the <code>ActionListener</code> that wants to
     *            register itself.
     */
    public synchronized void addActionListener(ActionListener listener) {
        actionListeners.addElement(listener);
    }

    /**
     * Remove an object that was being alerted when an item on this menu bar was
     * selected.
     * 
     * @param listener - the <code>ActionListener</code> that wants to be
     *            removed.
     */
    public synchronized void removeActionListener(ActionListener listener) {
        actionListeners.removeElement(listener);
    }

    /**
     * A helper function that will manage the enabled states of the items in
     * this menu, based upon the object's current state.
     */
    private synchronized void manageMenu() {
        // If we have a game, we can't join a new one, but we can save it.
        // Also, no Game menu in the editor (where (this.hasBoard &&
        // null==this.client)).
        if (game != null || hasBoard && null == client) {
            fileGameNew.setEnabled(false);
            fileGameOpen.setEnabled(false);
            fileGameScenario.setEnabled(false);
            fileGameConnectBot.setEnabled(false);
            fileGameConnect.setEnabled(false);
            // We can only save in certain phases of the game.
            if (phase != IGame.Phase.PHASE_UNKNOWN && phase != IGame.Phase.PHASE_LOUNGE
                    && phase != IGame.Phase.PHASE_SELECTION
                    && phase != IGame.Phase.PHASE_EXCHANGE
                    && phase != IGame.Phase.PHASE_VICTORY
                    && phase != IGame.Phase.PHASE_STARTING_SCENARIO) {
                fileGameSave.setEnabled(true);
            } else {
                fileGameSave.setEnabled(false);
            }
        }
        // If we have no game, we can't save, but we can create or join one.
        else {
            fileGameNew.setEnabled(true);
            fileGameOpen.setEnabled(true);
            fileGameSave.setEnabled(false);
            fileGameScenario.setEnabled(true);
            fileGameConnectBot.setEnabled(true);
            fileGameConnect.setEnabled(true);
        }

        // can view Game Opts if we have a game
        if (game != null) {
            viewGameOptions.setEnabled(true);
        } else {
            viewGameOptions.setEnabled(false);
        }

        // As of 2003-09-04, we can't ever print.
        filePrint.setEnabled(false);

        // the Client doesn't have any board actions
        if (client != null) {
            fileBoardNew.setEnabled(false);
            fileBoardOpen.setEnabled(false);
            fileBoardSave.setEnabled(false);
            fileBoardSaveAs.setEnabled(false);
            fileBoardSaveAsImage.setEnabled(false);
            // but the main window and map editor do
        } else {
            fileBoardNew.setEnabled(true);
            fileBoardOpen.setEnabled(true);
            fileBoardSave.setEnabled(false);
            fileBoardSaveAs.setEnabled(false);
            fileBoardSaveAsImage.setEnabled(false);
        }

        // If we have a board, we can perform board actions and view the mini
        // map.
        if (hasBoard) {
            // Save boards only in BoardEditor
            if (client == null) {
                fileBoardSave.setEnabled(true);
                fileBoardSaveAs.setEnabled(true);
                fileBoardSaveAsImage.setEnabled(true);
            }
            viewMiniMap.setEnabled(true);
            viewZoomIn.setEnabled(true);
            viewZoomOut.setEnabled(true);
        }
        // If we don't have a board we can't view the mini map.
        else {
            viewMiniMap.setEnabled(false);
            viewZoomIn.setEnabled(false);
            viewZoomOut.setEnabled(false);
        }

        // If we have a unit list, and if we are in the lounge,
        // then we can still perform all unit list actions.
        if (hasUnitList) {
            fileUnitsOpen.setEnabled(phase == IGame.Phase.PHASE_LOUNGE);
            fileUnitsClear.setEnabled(phase == IGame.Phase.PHASE_LOUNGE);
            fileUnitsSave.setEnabled(phase == IGame.Phase.PHASE_LOUNGE);
        }
        // If we don't have a unit list, but we are in the lounge,
        // then we can open a unit list.
        else {
            fileUnitsOpen.setEnabled(phase == IGame.Phase.PHASE_LOUNGE);
            fileUnitsClear.setEnabled(false);
            fileUnitsSave.setEnabled(false);
        }

        // If an entity has been selected, we can view it.
        if (entity != null) {
            viewMekDisplay.setEnabled(true);
        }
        // If we haven't selected an entity, we can't view it.
        else {
            viewMekDisplay.setEnabled(false);
        }

        // We can only view the LOS/Range tool setting and
        // the mini map in certain phases of the game. Also
        // the board editor has no LOS/Units/Player stuff
        if (client == null && hasBoard) {
            viewLOSSetting.setEnabled(false);
            viewUnitOverview.setEnabled(false);
            viewPlayerList.setEnabled(false);
        }
        // We're in-game.
        else if (phase == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES
                || phase == IGame.Phase.PHASE_DEPLOY_MINEFIELDS
                || phase == IGame.Phase.PHASE_MOVEMENT || phase == IGame.Phase.PHASE_FIRING
                || phase == IGame.Phase.PHASE_PHYSICAL
                || phase == IGame.Phase.PHASE_OFFBOARD
                || phase == IGame.Phase.PHASE_TARGETING
                || phase == IGame.Phase.PHASE_DEPLOYMENT) {
            viewLOSSetting.setEnabled(true);
            viewMiniMap.setEnabled(true);
            viewZoomIn.setEnabled(true);
            viewZoomOut.setEnabled(true);
            viewUnitOverview.setEnabled(true);
            viewPlayerList.setEnabled(true);
        }
        // We're in-game, but not in a phase with map functions.
        else {
            viewLOSSetting.setEnabled(false);
            viewMiniMap.setEnabled(false);
            viewZoomIn.setEnabled(false);
            viewZoomOut.setEnabled(false);
            viewUnitOverview.setEnabled(false);
            viewPlayerList.setEnabled(false);
        }

        // We can only view the round report in certain phases.
        if (phase == IGame.Phase.PHASE_INITIATIVE || phase == IGame.Phase.PHASE_MOVEMENT
                || phase == IGame.Phase.PHASE_FIRING || phase == IGame.Phase.PHASE_PHYSICAL
                || phase == IGame.Phase.PHASE_OFFBOARD
                || phase == IGame.Phase.PHASE_TARGETING || phase == IGame.Phase.PHASE_END
                || phase == IGame.Phase.PHASE_DEPLOYMENT) {
            viewRoundReport.setEnabled(true);
        } else {
            viewRoundReport.setEnabled(false);
        }

        // As of 2003-09-04, we can always at least look at the client settings.
        viewClientSettings.setEnabled(true);
        if (phase != IGame.Phase.PHASE_FIRING || entity == null) {
            fireCancel.setEnabled(false);
        } else {
            fireCancel.setEnabled(true);
        }
    }

    /**
     * Identify to the menu bar that a <code>IGame</code> is available to the
     * parent.
     * 
     * @param selected - The <code>IGame</code> that is currently selected.
     *            When there is no selection, set this to <code>null</code>.
     */
    public synchronized void setGame(IGame selected) {
        game = selected;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a <code>Board</code> is available to the
     * parent.
     * 
     * @param available - <code>true</code> when a <code>Board</code> is
     *            available. Set this value to <code>false</code> after the
     *            <code>Board</code> is cleared.
     */
    public synchronized void setBoard(boolean available) {
        hasBoard = available;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a unit list is available to the parent.
     * 
     * @param available - <code>true</code> when a unit list is available. Set
     *            this value to <code>false</code> after the unit list is
     *            cleared.
     */
    public synchronized void setUnitList(boolean available) {
        hasUnitList = available;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a <code>Entity</code> is available to the
     * parent.
     * 
     * @param selected - The <code>Entity</code> that is currently selected.
     *            When there is no selection, set this to <code>null</code>.
     */
    public synchronized void setEntity(Entity selected) {
        entity = selected;
        manageMenu();
    }

    /**
     * Identify to the menu bar which phase is currently in progress
     * 
     * @param current - the <code>int</code> value of the current phase (the
     *            valid values for this argument are defined as constants in the
     *            <code>Game</code> class).
     */
    public synchronized void setPhase(IGame.Phase current) {
        entity = null;
        phase = current;
        manageMenu();
    }

    // Manages the movement menu items...
    public synchronized void setMoveWalkEnabled(boolean enabled) {
        moveWalk.setEnabled(enabled);
    }

    public synchronized void setMoveTurnEnabled(boolean enabled) {
        moveTurn.setEnabled(enabled);
    }

    public synchronized void setMoveNextEnabled(boolean enabled) {
        moveNext.setEnabled(enabled);
    }

    public synchronized void setMoveLoadEnabled(boolean enabled) {
        moveLoad.setEnabled(enabled);
    }

    public synchronized void setMoveUnloadEnabled(boolean enabled) {
        moveUnload.setEnabled(enabled);
    }

    public synchronized void setMoveJumpEnabled(boolean enabled) {
        moveJump.setEnabled(enabled);
    }

    public synchronized void setMoveSwimEnabled(boolean enabled) {
        moveSwim.setEnabled(enabled);
    }

    public synchronized void setMoveLayMineEnabled(boolean enabled) {
        moveLayMine.setEnabled(enabled);
    }

    public synchronized void setMoveBackUpEnabled(boolean enabled) {
        moveBackUp.setEnabled(enabled);
    }

    public synchronized void setMoveChargeEnabled(boolean enabled) {
        moveCharge.setEnabled(enabled);
    }

    public synchronized void setMoveDFAEnabled(boolean enabled) {
        moveDFA.setEnabled(enabled);
    }

    public synchronized void setMoveGoProneEnabled(boolean enabled) {
        moveGoProne.setEnabled(enabled);
    }

    public synchronized void setMoveFleeEnabled(boolean enabled) {
        moveFlee.setEnabled(enabled);
    }

    public synchronized void setMoveEjectEnabled(boolean enabled) {
        moveEject.setEnabled(enabled);
    }

    public synchronized void setMoveUnjamEnabled(boolean enabled) {
        moveUnjam.setEnabled(enabled);
    }

    public synchronized void setMoveSearchlightEnabled(boolean enabled) {
        moveSearchlight.setEnabled(enabled);
    }

    public synchronized void setMoveHullDownEnabled(boolean enabled) {
        moveHullDown.setEnabled(enabled);
    }

    public synchronized void setMoveClearEnabled(boolean enabled) {
        moveClear.setEnabled(enabled);
    }

    public synchronized void setMoveGetUpEnabled(boolean enabled) {
        moveGetUp.setEnabled(enabled);
    }

    public synchronized void setMoveRaiseEnabled(boolean enabled) {
        moveRaise.setEnabled(enabled);
    }

    public synchronized void setMoveLowerEnabled(boolean enabled) {
        moveLower.setEnabled(enabled);
    }
    public synchronized void setMoveRecklessEnabled(boolean enabled) {
        moveReckless.setEnabled(enabled);
    }
    public synchronized void setMoveAccEnabled(boolean enabled) {
        moveAcc.setEnabled(enabled);
    }
    public synchronized void setMoveDecEnabled(boolean enabled) {
        moveDec.setEnabled(enabled);
    }
    public synchronized void setMoveAccNEnabled(boolean enabled) {
        moveAccN.setEnabled(enabled);
    }
    public synchronized void setMoveDecNEnabled(boolean enabled) {
        moveDecN.setEnabled(enabled);
    }
    public synchronized void setMoveEvadeEnabled(boolean enabled) {
        moveEvade.setEnabled(enabled);
    }
    public synchronized void setMoveEvadeAeroEnabled(boolean enabled) {
        moveEvadeAero.setEnabled(enabled);
    }
    public synchronized void setMoveRollEnabled(boolean enabled) {
        moveRoll.setEnabled(enabled);
    }
    public synchronized void setMoveLaunchEnabled(boolean enabled) {
        moveLaunch.setEnabled(enabled);
    }
    public synchronized void setMoveRecoverEnabled(boolean enabled) {
        moveRecover.setEnabled(enabled);
    }
    public synchronized void setMoveJoinEnabled(boolean enabled) {
        moveJoin.setEnabled(enabled);
    }
    public synchronized void setMoveDumpEnabled(boolean enabled) {
        moveDump.setEnabled(enabled);
    }
    public synchronized void setMoveRamEnabled(boolean enabled) {
        moveRam.setEnabled(enabled);
    }
    public synchronized void setMoveHoverEnabled(boolean enabled) {
        moveHover.setEnabled(enabled);
    }
    public synchronized void setMoveManeuverEnabled(boolean enabled) {
        moveManeuver.setEnabled(enabled);
    }
    public synchronized void setMoveTurnLeftEnabled(boolean enabled) {
        moveTurnLeft.setEnabled(enabled);
    }
    public synchronized void setMoveTurnRightEnabled(boolean enabled) {
        moveTurnRight.setEnabled(enabled);
    }
    public synchronized void setMoveThrustEnabled(boolean enabled) {
        moveThrust.setEnabled(enabled);
    }
    public synchronized void setMoveYawEnabled(boolean enabled) {
        moveYaw.setEnabled(enabled);
    }
    public synchronized void setMoveEndOverEnabled(boolean enabled) {
        moveEndOver.setEnabled(enabled);
    }

    // Manages deploy menu items...
    public synchronized void setDeployNextEnabled(boolean enabled) {
        deployNext.setEnabled(enabled);
    }

    public synchronized void setDeployTurnEnabled(boolean enabled) {
        deployTurn.setEnabled(enabled);
    }

    public synchronized void setDeployLoadEnabled(boolean enabled) {
        deployLoad.setEnabled(enabled);
    }

    public synchronized void setDeployUnloadEnabled(boolean enabled) {
        deployUnload.setEnabled(enabled);
    }

    public synchronized void setDeployRemoveEnabled(boolean enabled) {
        deployRemove.setEnabled(enabled);
    }

    public synchronized void setDeployAssaultDropEnabled(boolean enabled) {
        deployAssaultDrop.setEnabled(enabled);
    }

    // Manages deploy minefield items...
    public synchronized void setDeployConventionalEnabled(int nbr) {
        deployMinesConventional.setText(Messages.getString(
                "CommonMenuBar.Minefield", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        deployMinesConventional.setEnabled(nbr > 0);
    }

    public synchronized void setDeployCommandEnabled(int nbr) {
        deployMinesCommand.setText(Messages.getString(
                "CommonMenuBar.Command", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        // Cannot ever deploy command mines...
        deployMinesCommand.setEnabled(false);
    }

    public synchronized void setDeployVibrabombEnabled(int nbr) {
        deployMinesVibrabomb.setText(Messages.getString(
                "CommonMenuBar.Vibrabomb", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        deployMinesVibrabomb.setEnabled(nbr > 0);
    }
    
    public synchronized void setDeployActiveEnabled(int nbr) {
        deployMinesActive.setText(Messages.getString(
                "CommonMenuBar.Active", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        deployMinesActive.setEnabled(nbr > 0);
    }
    
    public synchronized void setDeployInfernoEnabled(int nbr) {
        deployMinesInferno.setText(Messages.getString(
                "CommonMenuBar.Inferno", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        deployMinesInferno.setEnabled(nbr > 0);
    }

    // Manages physical menu items...
    public synchronized void setPhysicalNextEnabled(boolean enabled) {
        physicalNext.setEnabled(enabled);
    }

    public synchronized void setPhysicalPunchEnabled(boolean enabled) {
        physicalPunch.setEnabled(enabled);
    }

    public synchronized void setPhysicalKickEnabled(boolean enabled) {
        physicalKick.setEnabled(enabled);
    }

    public synchronized void setPhysicalPushEnabled(boolean enabled) {
        physicalPush.setEnabled(enabled);
    }

    public synchronized void setPhysicalClubEnabled(boolean enabled) {
        physicalClub.setEnabled(enabled);
    }

    public synchronized void setPhysicalBrushOffEnabled(boolean enabled) {
        physicalBrushOff.setEnabled(enabled);
    }

    public synchronized void setPhysicalDodgeEnabled(boolean enabled) {
        physicalDodge.setEnabled(enabled);
    }

    public synchronized void setPhysicalThrashEnabled(boolean enabled) {
        physicalThrash.setEnabled(enabled);
    }

    public synchronized void setPhysicalProtoEnabled(boolean enabled) {
        physicalProto.setEnabled(enabled);
    }

    // Manages fire menu items...

    public synchronized void setFireFireEnabled(boolean enabled) {
        fireFire.setEnabled(enabled);
    }

    public synchronized void setFireSkipEnabled(boolean enabled) {
        fireSkip.setEnabled(enabled);
    }

    public synchronized void setFireNextTargetEnabled(boolean enabled) {
        fireNextTarg.setEnabled(enabled);
    }

    public synchronized void setFireNextEnabled(boolean enabled) {
        fireNext.setEnabled(enabled);
    }

    public synchronized void setFireTwistEnabled(boolean enabled) {
        fireTwist.setEnabled(enabled);
    }

    public synchronized void setFireFlipArmsEnabled(boolean enabled) {
        fireFlipArms.setEnabled(enabled);
    }

    public synchronized void setFireModeEnabled(boolean enabled) {
        fireMode.setEnabled(enabled);
    }

    public synchronized void setFireFindClubEnabled(boolean enabled) {
        fireFindClub.setEnabled(enabled);
    }

    public synchronized void setFireSpotEnabled(boolean enabled) {
        fireSpot.setEnabled(enabled);
    }

    public synchronized void setFireSearchlightEnabled(boolean enabled) {
        fireSearchlight.setEnabled(enabled);
    }

    public synchronized void setFireClearTurretEnabled(boolean enabled) {
        fireClearTurret.setEnabled(enabled);
    }

    public synchronized void setFireClearWeaponJamEnabled(boolean enabled) {
        fireClearWeaponJam.setEnabled(enabled);
    }
}
