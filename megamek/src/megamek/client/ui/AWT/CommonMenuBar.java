/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.common.*;

/**
 * Every menu bar in MegaMek should have an identical look-and-feel,
 * with various menu items enabled or disabled, based upon the frame
 * that owns the menu bar, and the current state of the program.
 */
public class CommonMenuBar extends MenuBar implements ActionListener, KeyListener
{
    /**
     * The <code>Game</code> current selected.
     * This value may be <code>null</code>.
     */
    private Game game = null;
    private boolean isJ2RE;

    private MenuItem fileGameNew = null;
    private MenuItem fileGameOpen = null;
    private MenuItem fileGameSave = null;
    private MenuItem fileGameScenario = null;
    private MenuItem fileGameConnectBot = null;
    private MenuItem fileGameConnect = null;

    /**
     * When we have a <code>Board</code>, set this to <code>true</code>.
     */
    private boolean hasBoard = false;

    private MenuItem fileBoardNew = null;
    private MenuItem fileBoardOpen = null;
    private MenuItem fileBoardSave = null;
    private MenuItem fileBoardSaveAs = null;
    private MenuItem fileBoardSaveAsImage = null;

    /**
     * When we have a unit list, set this to <code>true</code>.
     */
    private boolean hasUnitList = false;

    private MenuItem fileUnitsOpen = null;
    private MenuItem fileUnitsClear = null;
    private MenuItem fileUnitsSave = null;

    /**
     * The <code>Entity</code> current selected.
     * This value may be <code>null</code>.
     */
    private Entity entity = null;

    /**
     * Record the current phase of the game.
     */
    private int phase = Game.PHASE_UNKNOWN;

    private MenuItem filePrint = null;

    private MenuItem viewMiniMap = null;
    private MenuItem viewMekDisplay = null;
    private MenuItem viewZoomIn = null;
    private MenuItem viewZoomOut = null;
    private MenuItem viewLOSSetting = null;
    private MenuItem viewUnitOverview = null;
    private MenuItem viewInitiativeReport = null;
    private MenuItem viewTurnReport = null;
    private MenuItem viewGameOptions = null;
    private MenuItem viewClientSettings = null;
    private MenuItem viewPlayerList = null;

    private MenuItem deployMinesConventional = null;
    private MenuItem deployMinesCommand = null;
    private MenuItem deployMinesVibrabomb = null;
    private MenuItem deployNext = null;
    private MenuItem deployTurn = null;
    private MenuItem deployLoad = null;
    private MenuItem deployUnload = null;

    private MenuItem moveWalk = null;
    private MenuItem moveNext = null;
    private MenuItem moveTurn = null;
    private MenuItem moveLoad = null;
    private MenuItem moveUnload = null;
    private MenuItem moveJump = null;
    private MenuItem moveBackUp = null;
    private MenuItem moveCharge = null;
    private MenuItem moveDFA = null;
    private MenuItem moveGoProne = null;
    private MenuItem moveFlee = null;
    private MenuItem moveEject = null;
    private MenuItem moveUnjam = null;
    private MenuItem moveClear = null;
    private MenuItem moveGetUp = null;

    private MenuItem fireFire           = null;
    private MenuItem fireSkip           = null;
    private MenuItem fireNextTarg       = null;
    private MenuItem fireNext           = null;
    private MenuItem fireTwist          = null;
    private MenuItem fireFlipArms       = null;
    private MenuItem fireMode           = null;
    private MenuItem fireFindClub       = null;
    private MenuItem fireSpot           = null;
    private MenuItem fireCancel         = null;

    private MenuItem physicalNext       = null;
    private MenuItem physicalPunch      = null;
    private MenuItem physicalKick       = null;
    private MenuItem physicalPush       = null;
    private MenuItem physicalClub       = null;
    private MenuItem physicalBrushOff   = null;
    private MenuItem physicalDodge      = null;
    private MenuItem physicalThrash     = null;
    private MenuItem physicalProto      = null;

    private Client client;

    /**
     * A <code>Vector</code> containing the <code>ActionListener</code>s
     * that have registered themselves with this menu bar.
     */
    private final Vector actionListeners = new Vector();

    /**
     * Create a MegaMek menu bar.
     */
    public CommonMenuBar(Client parent) {
        this();
        client=parent;
    };

    public CommonMenuBar() {
        Menu menu = null;
        Menu submenu = null;
        MenuItem item = null;

        // *** Create the File menu.
        menu = new Menu( "File" );
        this.add( menu );

     	Properties p = System.getProperties();
     	String javaVersion = p.getProperty( "java.version" );
     	if ( javaVersion.length() < 3 || javaVersion.charAt(2) == '1' ){
            isJ2RE = false;
     	} else {
            isJ2RE = true;
     	}
        
        // Create the Game sub-menu.
        submenu = new Menu( "Game" );
        menu.add( submenu );
        fileGameNew = new MenuItem( "New" );
        fileGameNew.addActionListener( this );
        fileGameNew.setActionCommand( "fileGameNew" );
        submenu.add( fileGameNew );
        fileGameOpen = new MenuItem( "Open..." );
        fileGameOpen.addActionListener( this );
        fileGameOpen.setActionCommand( "fileGameOpen" );
        submenu.add( fileGameOpen );
        fileGameSave = new MenuItem( "Save..." );
        fileGameSave.addActionListener( this );
        fileGameSave.setActionCommand( "fileGameSave" );
        submenu.add( fileGameSave );
        submenu.addSeparator();
        fileGameScenario = new MenuItem( "Open Scenario..." );
        fileGameScenario.addActionListener( this );
        fileGameScenario.setActionCommand( "fileGameScenario" );
        submenu.add( fileGameScenario );
        submenu.addSeparator();
        fileGameConnectBot = new MenuItem( "Connect as Bot..." );
        fileGameConnectBot.addActionListener( this );
        fileGameConnectBot.setActionCommand( "fileGameConnectBot" );
        submenu.add( fileGameConnectBot );
        fileGameConnect = new MenuItem( "Connect..." );
        fileGameConnect.addActionListener( this );
        fileGameConnect.setActionCommand( "fileGameConnect" );
        submenu.add( fileGameConnect );

        // Create the Board sub-menu.
        submenu = new Menu( "Board" );
        menu.add( submenu );
        fileBoardNew = new MenuItem( "New" );
        fileBoardNew.addActionListener( this );
        fileBoardNew.setActionCommand( "fileBoardNew" );
        submenu.add( fileBoardNew );
        fileBoardOpen = new MenuItem( "Open..." );
        fileBoardOpen.addActionListener( this );
        fileBoardOpen.setActionCommand( "fileBoardOpen" );
        submenu.add( fileBoardOpen );
        fileBoardSave = new MenuItem( "Save..." );
        fileBoardSave.addActionListener( this );
        fileBoardSave.setActionCommand( "fileBoardSave" );
        submenu.add( fileBoardSave );
        fileBoardSaveAs = new MenuItem( "Save As..." );
        fileBoardSaveAs.addActionListener( this );
        fileBoardSaveAs.setActionCommand( "fileBoardSaveAs" );
        submenu.add( fileBoardSaveAs );
        fileBoardSaveAsImage = new MenuItem( "Save As Image..." );
        fileBoardSaveAsImage.addActionListener( this );
        fileBoardSaveAsImage.setActionCommand( "fileBoardSaveAsImage" );
        submenu.add( fileBoardSaveAsImage );

        // Create the Unit List sub-menu.
        submenu = new Menu( "Unit List" );
        menu.add( submenu );
        fileUnitsOpen = new MenuItem( "Open..." );
        fileUnitsOpen.addActionListener( this );
        fileUnitsOpen.setActionCommand( "fileUnitsOpen" );
        submenu.add( fileUnitsOpen );
        fileUnitsClear = new MenuItem( "Clear" );
        fileUnitsClear.addActionListener( this );
        fileUnitsClear.setActionCommand( "fileUnitsClear" );
        submenu.add( fileUnitsClear );
        fileUnitsSave = new MenuItem( "Save..." );
        fileUnitsSave.addActionListener( this );
        fileUnitsSave.setActionCommand( "fileUnitsSave" );
        submenu.add( fileUnitsSave );

        // Finish off the File menu.
        filePrint = new MenuItem( "Print" );
        filePrint.addActionListener( this );
        filePrint.setActionCommand( "filePrint" );
        filePrint.setEnabled( false );
        menu.addSeparator();
        menu.add( filePrint );

        // *** Create the view menu.
        menu = new Menu( "View" );
        this.add( menu );

        viewMekDisplay = new MenuItem( "Mek Display" );
        viewMekDisplay.addActionListener( this );
        viewMekDisplay.setActionCommand(ClientGUI.VIEW_MEK_DISPLAY);
        viewMekDisplay.setShortcut(new MenuShortcut(KeyEvent.VK_D));
        menu.add( viewMekDisplay );
        viewMiniMap = new MenuItem( "Mini Map" );
        viewMiniMap.addActionListener( this );
        viewMiniMap.setActionCommand(ClientGUI.VIEW_MINI_MAP);
        viewMiniMap.setShortcut(new MenuShortcut(KeyEvent.VK_M));
        menu.add( viewMiniMap );
        viewUnitOverview = new MenuItem( "Unit Overview" );
        viewUnitOverview.addActionListener( this );
        viewUnitOverview.setActionCommand(ClientGUI.VIEW_UNIT_OVERVIEW);
        viewUnitOverview.setShortcut(new MenuShortcut(KeyEvent.VK_U));
        menu.add( viewUnitOverview );
        viewZoomIn = new MenuItem( "Zoom In" );
        viewZoomIn.addActionListener( this );
        viewZoomIn.setActionCommand(ClientGUI.VIEW_ZOOM_IN);
        menu.add( viewZoomIn );
        viewZoomOut = new MenuItem( "Zoom Out" );
        viewZoomOut.addActionListener( this );
        viewZoomOut.setActionCommand(ClientGUI.VIEW_ZOOM_OUT);
        menu.add( viewZoomOut );
        menu.addSeparator();

        viewTurnReport = new MenuItem( "Turn Report" );
        viewTurnReport.addActionListener( this );
        viewTurnReport.setActionCommand( "viewTurnReport" );
        viewTurnReport.setShortcut(new MenuShortcut(KeyEvent.VK_R));
        menu.add( viewTurnReport );

        viewInitiativeReport = new MenuItem( "Initiative Report" );
        viewInitiativeReport.addActionListener( this );
        viewInitiativeReport.setActionCommand( "viewInitiativeReport" );
        menu.add( viewInitiativeReport );
        menu.addSeparator();
        viewGameOptions = new MenuItem( "Game Options" );
        viewGameOptions.setActionCommand( "viewGameOptions" );
        viewGameOptions.addActionListener( this );
        menu.add( viewGameOptions );
        viewClientSettings = new MenuItem( "Client Settings" );
        viewClientSettings.setActionCommand( "viewClientSettings" );
        viewClientSettings.addActionListener( this );
        menu.add( viewClientSettings );
        viewLOSSetting = new MenuItem( "LOS Setting" );
        viewLOSSetting.addActionListener( this );
        viewLOSSetting.setActionCommand(ClientGUI.VIEW_LOS_SETTING);
        viewLOSSetting.setShortcut(new MenuShortcut(KeyEvent.VK_L));
        menu.add( viewLOSSetting );
        menu.addSeparator();
        viewPlayerList = new MenuItem( "Player List" );
        viewPlayerList.setActionCommand( "viewPlayerList" );
        viewPlayerList.addActionListener( this );
        menu.add( viewPlayerList );

        // *** Create the deployo menu.
        menu = new Menu( "Deploy" );
        this.add( menu );

        // Create the Mines sub-menu.
        submenu = new Menu( "Mines" );

        deployMinesConventional = createMenuItem(submenu, "Conventional", DeployMinefieldDisplay.DEPLOY_MINE_CONV);
        deployMinesCommand = createMenuItem(submenu, "Command", DeployMinefieldDisplay.DEPLOY_MINE_COM);
        deployMinesVibrabomb = createMenuItem(submenu, "Vibrabomb", DeployMinefieldDisplay.DEPLOY_MINE_VIBRA);

        // Finish off the deploy menu.
        deployNext = createMenuItem(menu, "Next Unit", DeploymentDisplay.DEPLOY_NEXT, KeyEvent.VK_N);
        deployTurn = createMenuItem(menu, "Turn", DeploymentDisplay.DEPLOY_TURN);
        deployLoad = createMenuItem(menu, "Load", DeploymentDisplay.DEPLOY_LOAD);
        deployUnload = createMenuItem(menu, "Unload", DeploymentDisplay.DEPLOY_UNLOAD);

        menu.addSeparator();

        menu.add( submenu );

        // *** Create the move menu.
        menu = new Menu( "Move" );
        this.add( menu );

        moveWalk = createMenuItem(menu, "Walk", MovementDisplay.MOVE_WALK, KeyEvent.VK_W);
        moveJump = createMenuItem(menu, "Jump", MovementDisplay.MOVE_JUMP, KeyEvent.VK_J);
        moveBackUp = createMenuItem(menu, "Back Up", MovementDisplay.MOVE_BACK_UP);
        moveGetUp = createMenuItem(menu, "Get Up", MovementDisplay.MOVE_GET_UP);
        moveGoProne = createMenuItem(menu, "Go Prone", MovementDisplay.MOVE_GO_PRONE);
        moveTurn = createMenuItem(menu, "Turn", MovementDisplay.MOVE_TURN);
        moveNext = createMenuItem(menu, "Next Unit", MovementDisplay.MOVE_NEXT, KeyEvent.VK_N);

        // Create the Special sub-menu.
        submenu = new Menu( "Special" );

        moveLoad = createMenuItem(submenu, "Load", MovementDisplay.MOVE_LOAD);
        moveUnload = createMenuItem(submenu, "Unload", MovementDisplay.MOVE_UNLOAD);
        submenu.addSeparator();
        moveCharge = createMenuItem(submenu, "Charge", MovementDisplay.MOVE_CHARGE);
        moveDFA = createMenuItem(submenu, "Death From Above", MovementDisplay.MOVE_DFA);
        submenu.addSeparator();
        moveFlee = createMenuItem(submenu, "Flee", MovementDisplay.MOVE_FLEE);
        moveEject = createMenuItem(submenu, "Eject", MovementDisplay.MOVE_EJECT);
        submenu.addSeparator();
        moveUnjam = createMenuItem(submenu, "Unjam RAC", MovementDisplay.MOVE_UNJAM);
        moveClear = createMenuItem(submenu, "Clear Minefield", MovementDisplay.MOVE_CLEAR);

        menu.addSeparator();
        menu.add( submenu );

        // Add the cancel button.
        menu.addSeparator();
        moveNext = createMenuItem(menu, "Cancel", MovementDisplay.MOVE_CANCEL, KeyEvent.VK_ESCAPE);

        // *** Create the fire menu.
        menu = new Menu( "Fire" );
        this.add( menu );

        fireFire = createMenuItem(menu, "Fire", FiringDisplay.FIRE_FIRE, KeyEvent.VK_F);
        fireSkip = createMenuItem(menu, "Skip", FiringDisplay.FIRE_SKIP, KeyEvent.VK_S);
        fireNextTarg = createMenuItem(menu, "Next Target", FiringDisplay.FIRE_NEXT_TARG, KeyEvent.VK_T);
        fireNext = createMenuItem(menu, "Next Unit", FiringDisplay.FIRE_NEXT, KeyEvent.VK_N);

        menu.addSeparator();

        fireTwist = createMenuItem(menu, "Twist", FiringDisplay.FIRE_TWIST);
        fireFlipArms = createMenuItem(menu, "Flip Arms", FiringDisplay.FIRE_FLIP_ARMS);

        menu.addSeparator();

		fireMode = createMenuItem(menu, "Mode", FiringDisplay.FIRE_MODE, KeyEvent.VK_O);

        menu.addSeparator();

		fireFindClub = createMenuItem(menu, "Find Club", FiringDisplay.FIRE_FIND_CLUB);
		fireSpot = createMenuItem(menu, "Spot", FiringDisplay.FIRE_SPOT);

        menu.addSeparator();

		fireCancel = createMenuItem(menu, "Cancel", FiringDisplay.FIRE_CANCEL, KeyEvent.VK_ESCAPE);

        // *** Create the physical menu.
        menu = new Menu( "Physical" );
        this.add( menu );

        physicalPunch = createMenuItem(menu, "Punch", PhysicalDisplay.PHYSICAL_PUNCH);
        physicalKick = createMenuItem(menu, "Kick", PhysicalDisplay.PHYSICAL_KICK);
        physicalPush = createMenuItem(menu, "Push", PhysicalDisplay.PHYSICAL_PUSH);
        physicalClub = createMenuItem(menu, "Club", PhysicalDisplay.PHYSICAL_CLUB);
        physicalBrushOff = createMenuItem(menu, "Brush Off", PhysicalDisplay.PHYSICAL_BRUSH_OFF);
        physicalThrash = createMenuItem(menu, "Thrash", PhysicalDisplay.PHYSICAL_THRASH);
        physicalProto = createMenuItem(menu, "Protomech Physical", PhysicalDisplay.PHYSICAL_PROTO);
        physicalDodge = createMenuItem(menu, "Dodge", PhysicalDisplay.PHYSICAL_DODGE);
        physicalNext = createMenuItem(menu, "Next Unit", PhysicalDisplay.PHYSICAL_NEXT, KeyEvent.VK_N);

        // *** Create the help menu.
        menu = new Menu( "Help" );
        this.setHelpMenu( menu );

        item = new MenuItem( "Contents" );
        item.addActionListener( this );
        item.setActionCommand( "helpContents" );
        menu.add( item );
        menu.addSeparator();
        item = new MenuItem( "About MegaMek" );;
        item.setActionCommand( "helpAbout" );
        item.addActionListener( this );
        menu.add( item );

        // Now manage the menu items.
        manageMenu();
    }

	private MenuItem createMenuItem(Menu m, String label, String command, int shortcut) {
		MenuItem mi = createMenuItem(m, label, command);
		mi.setShortcut(new MenuShortcut(shortcut));
		return mi;
	}

	private MenuItem createMenuItem(Menu m, String label, String command) {
        MenuItem mi = new MenuItem( label );
        mi.addActionListener( this );
        mi.setActionCommand(command);
        mi.setEnabled(false);
        m.add( mi );
        return mi;
	}

    /**
     * Implement the <code>ActionListener</code> interface.
     *
     * @param   event - the <code>ActionEvent</code> that spawned this call.
     */
    public void actionPerformed( ActionEvent event ) {
        // Pass the action on to each of our listeners.
        Enumeration iter = this.actionListeners.elements();
        while ( iter.hasMoreElements() ) {
            ActionListener listener = (ActionListener) iter.nextElement();
            listener.actionPerformed( event );
        }
    }

    /**
     * Register an object that wishes to be alerted when an item on this
     * menu bar has been selected.
     * <p/>
     * Please note, the ActionCommand property of the action event  will
     * inform the listener as to which menu item that has been selected.
     * Not all listeners will be interested in all menu items.
     *
     * @param   listener - the <code>ActionListener</code> that wants to
     *          register itself.
     */
    public synchronized void addActionListener( ActionListener listener ) {
        this.actionListeners.addElement( listener );
    }

    /**
     * Remove an object that was being alerted when an item on this
     * menu bar was selected.
     *
     * @param   listener - the <code>ActionListener</code> that wants to
     *          be removed.
     */
    public synchronized void removeActionListener( ActionListener listener ) {
        this.actionListeners.removeElement( listener );
    }

    /**
     * A helper function that will manage the enabled states of the items
     * in this menu, based upon the object's current state.
     */
    private synchronized void manageMenu() {
        // If we have a game, we can't join a new one, but we can save it.
        // Also, no Game menu in the editor (where (this.hasBoard && null==this.client)).
        if ( this.game != null || (this.hasBoard && null==this.client)) {
            fileGameNew.setEnabled( false );
            fileGameOpen.setEnabled( false );
            fileGameScenario.setEnabled( false );
            fileGameConnectBot.setEnabled( false );
            fileGameConnect.setEnabled( false );
            // We can only save in certain phases of the game.
            if ( this.phase != Game.PHASE_UNKNOWN &&
                 this.phase != Game.PHASE_LOUNGE &&
                 this.phase != Game.PHASE_SELECTION &&
                 this.phase != Game.PHASE_EXCHANGE &&
                 this.phase != Game.PHASE_VICTORY &&
                 this.phase != Game.PHASE_STARTING_SCENARIO ) {
                fileGameSave.setEnabled( true );
            } else {
                fileGameSave.setEnabled( false );
            }
        }
        // If we have no game, we can't save, but we can create or join one.
        else { 
                fileGameNew.setEnabled( true );
                fileGameOpen.setEnabled( true );
                fileGameSave.setEnabled( false );
                fileGameScenario.setEnabled( true );
                fileGameConnectBot.setEnabled( true );
                fileGameConnect.setEnabled( true );
        }

        // can view Game Opts if we have a game
        if ( this.game != null ) {
            viewGameOptions.setEnabled( true );
        } else {
            viewGameOptions.setEnabled( false );
        };

        // As of 2003-09-04, we can't ever print.
        filePrint.setEnabled( false );

        // the Client doesn't have any board actions
        if (null!=client) {
                fileBoardNew.setEnabled( false );
                fileBoardOpen.setEnabled( false );
                fileBoardSave.setEnabled( false );
                fileBoardSaveAs.setEnabled( false );
                fileBoardSaveAsImage.setEnabled( false );
        // but the main window and map editor do
        } else {
            fileBoardNew.setEnabled( true );
            fileBoardOpen.setEnabled( true );
            fileBoardSave.setEnabled( false );
            fileBoardSaveAs.setEnabled( false );
            fileBoardSaveAsImage.setEnabled( false );
        };

        // If we have a board, we can perform board actions and view the mini map.
        if ( this.hasBoard ) {
            // Save boards only in BoardEditor
            if (null==client) {
                fileBoardSave.setEnabled( true );
                fileBoardSaveAs.setEnabled( true );
                fileBoardSaveAsImage.setEnabled( true );
            };
            viewMiniMap.setEnabled( true );
            if ( isJ2RE ){
            	viewZoomIn.setEnabled( true );
            	viewZoomOut.setEnabled( true );
            } else {
            	viewZoomIn.setEnabled( false);
            	viewZoomOut.setEnabled( false );
            }
        }
        // If we don't have a board we can't view the mini map.
        else {
            viewMiniMap.setEnabled( false );
            viewZoomIn.setEnabled( false );
            viewZoomOut.setEnabled( false );
        }

        // If we have a unit list, and if we are in the lounge,
        // then we can still perform all unit list actions.
        if ( this.hasUnitList ) {
            fileUnitsOpen.setEnabled( (this.phase == Game.PHASE_LOUNGE) );
            fileUnitsClear.setEnabled( (this.phase == Game.PHASE_LOUNGE) );
            fileUnitsSave.setEnabled( (this.phase == Game.PHASE_LOUNGE) );
        }
        // If we don't have a unit list, but we are in the lounge,
        // then we can open a unit list.
        else {
            fileUnitsOpen.setEnabled( (this.phase == Game.PHASE_LOUNGE) );
            fileUnitsClear.setEnabled( false );
            fileUnitsSave.setEnabled( false );
        }

        // If an entity has been selected, we can view it.
        if ( this.entity != null ) {
            viewMekDisplay.setEnabled( true );
        }
        // If we haven't selected an entity, we can't view it.
        else {
            viewMekDisplay.setEnabled( false );
        }

        // We can only view the LOS/Range tool setting and
        // the mini map in certain phases.
        if ( this.phase == Game.PHASE_SET_ARTYAUTOHITHEXES ||
             this.phase == Game.PHASE_DEPLOY_MINEFIELDS ||
             this.phase == Game.PHASE_MOVEMENT ||
             this.phase == Game.PHASE_FIRING ||
             this.phase == Game.PHASE_PHYSICAL ||
             this.phase == Game.PHASE_OFFBOARD ||
             this.phase == Game.PHASE_TARGETING ||
             this.phase == Game.PHASE_DEPLOYMENT ) {
            viewLOSSetting.setEnabled( true );
            viewMiniMap.setEnabled( true );
            if ( isJ2RE == true ){
	            viewZoomIn.setEnabled( true );
	            viewZoomOut.setEnabled( true );
            } else {
	            viewZoomIn.setEnabled( false );
	            viewZoomOut.setEnabled( false );
            }
            viewUnitOverview.setEnabled( true );
            viewPlayerList.setEnabled( true );
        } else {
            viewLOSSetting.setEnabled( false );
            viewMiniMap.setEnabled( false );
            viewZoomIn.setEnabled( false );
            viewZoomOut.setEnabled( false );
            viewUnitOverview.setEnabled( false );
            viewPlayerList.setEnabled( false );
        }

        // We can only view the turn report in certain phases.
        if ( this.phase == Game.PHASE_INITIATIVE ||
             this.phase == Game.PHASE_MOVEMENT ||
             this.phase == Game.PHASE_FIRING ||
             this.phase == Game.PHASE_PHYSICAL ||
             this.phase == Game.PHASE_OFFBOARD ||
             this.phase == Game.PHASE_TARGETING ||
             this.phase == Game.PHASE_END ||
             this.phase == Game.PHASE_DEPLOYMENT ) {
            viewTurnReport.setEnabled( true );
        } else {
            viewTurnReport.setEnabled( false );
        }

        // As of 2003-09-04, we can't ever view the initiative report.
        viewInitiativeReport.setEnabled( false );

        // As of 2003-09-04, we can always at least look at the client settings.
        viewClientSettings.setEnabled( true );

       if ( this.phase != Game.PHASE_FIRING || entity == null ) {
            fireCancel.setEnabled( false );
        } else {
            fireCancel.setEnabled( true );
        }
    }

    /**
     * Identify to the menu bar that a <code>Game</code> is available to the
     * parent.
     *
     * @param   selected - The <code>Game</code> that is currently selected.
     *          When there is no selection, set this to <code>null</code>.
     */
    public synchronized void setGame( Game selected ) {
        this.game = selected;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a <code>Board</code> is available to the
     * parent.
     *
     * @param   available - <code>true</code> when a <code>Board</code> is
     *          available.  Set this value to <code>false</code> after
     *          the <code>Board</code> is cleared.
     */
    public synchronized void setBoard( boolean available ) {
        this.hasBoard = available;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a unit list is available to the
     * parent.
     *
     * @param   available - <code>true</code> when a unit list is
     *          available.  Set this value to <code>false</code> after
     *          the unit list is cleared.
     */
    public synchronized void setUnitList( boolean available ) {
        this.hasUnitList = available;
        manageMenu();
    }

    /**
     * Identify to the menu bar that a <code>Entity</code> is available to the
     * parent.
     *
     * @param   selected - The <code>Entity</code> that is currently selected.
     *          When there is no selection, set this to <code>null</code>.
     */
    public synchronized void setEntity( Entity selected ) {
        this.entity = selected;
        manageMenu();
    }

    /**
     * Identify to the menu bar which phase is currently in progress
     *
     * @param   current - the <code>int</code> value of the current
     *          phase (the valid values for this argument are defined
     *          as constants in the <code>Game</code> class).
     */
    public synchronized void setPhase( int current ) {
    	this.entity = null;
        this.phase = current;
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
	public synchronized void setMoveClearEnabled(boolean enabled) {
    	moveClear.setEnabled(enabled);
	}
	public synchronized void setMoveGetUpEnabled(boolean enabled) {
    	moveGetUp.setEnabled(enabled);
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

	// Manages deploy minefield items...
	public synchronized void setDeployConventionalEnabled(int nbr) {
        deployMinesConventional.setLabel("Minefield(" + nbr + ")");
        deployMinesConventional.setEnabled(nbr > 0);
	}
	public synchronized void setDeployCommandEnabled(int nbr) {
        deployMinesCommand.setLabel("Command(" + nbr + ")");
        // Cannot ever deploy command mines...
        deployMinesCommand.setEnabled(false);
	}
	public synchronized void setDeployVibrabombEnabled(int nbr) {
        deployMinesVibrabomb.setLabel("Vibrabomb(" + nbr + ")");
        deployMinesVibrabomb.setEnabled(nbr > 0);
	}

	//Manages physical menu items...
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
	//Manages fire menu items...

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

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        // handle pseudo--menu-shortcuts
        if (ev.isControlDown()) {
            // for every menu accelerator...
            for (Enumeration shortcuts = shortcuts(); shortcuts.hasMoreElements() ;) {
                MenuShortcut shortcut = (MenuShortcut) shortcuts.nextElement();

                // is this keyPress the same as a menu accelerator?
                if ((shortcut.getKey() == ev.getKeyCode()) && (shortcut.usesShiftModifier() == ev.isShiftDown()) ) {
                    // fire off the menu action event if the menu is active
                    if (getShortcutMenuItem(shortcut).isEnabled()) {
                        actionPerformed( new ActionEvent(this,1,getShortcutMenuItem(shortcut).getActionCommand()) );
                    };
                };
            };
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }

}
