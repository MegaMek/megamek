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
public class CommonMenuBar extends MenuBar implements ActionListener
{
    /**
     * The <code>Game</code> current selected.
     * This value may be <code>null</code>.
     */
    private Game game = null;

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
    private MenuItem viewLOSSetting = null;
    private MenuItem viewInitiativeReport = null;
    private MenuItem viewTurnReport = null;
    private MenuItem viewGameOptions = null;
    private MenuItem viewClientSettings = null;

	private int nbrConv = 0;
	private int nbrCommand = 0;
	private int nbrVibra = 0;	

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

    /**
     * When we have selected a weapon that can fire, set this to <code>true</code>.
     */
    private boolean hasFireChoice = false;

    /**
     * When we have a visible target, set this to <code>true</code>.
     */
    private boolean hasTarget = false;

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

    /**
     * A <code>Vector</code> containing the <code>ActionListener</code>s
     * that have registered themselves with this menu bar.
     */
    private final Vector actionListeners = new Vector();

    /**
     * Create a MegaMek menu bar.
     */
    public CommonMenuBar() {
        Menu menu = null;
        Menu submenu = null;
        MenuItem item = null;

        // *** Create the File menu.
        menu = new Menu( "File" );
        this.add( menu );

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
        viewMekDisplay.setActionCommand(AbstractPhaseDisplay.VIEW_MEK_DISPLAY);
        menu.add( viewMekDisplay );
        viewMiniMap = new MenuItem( "Mini Map" );
        viewMiniMap.addActionListener( this );
        viewMiniMap.setActionCommand(AbstractPhaseDisplay.VIEW_MINI_MAP);
        menu.add( viewMiniMap );
        viewLOSSetting = new MenuItem( "LOS Setting" );
        viewLOSSetting.addActionListener( this );
        viewLOSSetting.setActionCommand(AbstractPhaseDisplay.VIEW_LOS_SETTING);
        menu.add( viewLOSSetting );
        menu.addSeparator();
        viewTurnReport = new MenuItem( "Turn Report" );
        viewTurnReport.addActionListener( this );
        viewTurnReport.setActionCommand( "viewTurnReport" );
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

        // *** Create the deployo menu.
        menu = new Menu( "Deploy" );
        this.add( menu );

        // Create the Mines sub-menu.
        submenu = new Menu( "Mines" );

        deployMinesConventional = new MenuItem( "Conventional" );
        deployMinesConventional.addActionListener( this );
        deployMinesConventional.setActionCommand( DeployMinefieldDisplay.DEPLOY_MINE_CONV );
        submenu.add( deployMinesConventional );
        deployMinesCommand = new MenuItem( "Command" );
        deployMinesCommand.addActionListener( this );
        deployMinesCommand.setActionCommand( DeployMinefieldDisplay.DEPLOY_MINE_COM );
        submenu.add( deployMinesCommand );
        deployMinesVibrabomb = new MenuItem( "Vibrabomb" );
        deployMinesVibrabomb.addActionListener( this );
        deployMinesVibrabomb.setActionCommand( DeployMinefieldDisplay.DEPLOY_MINE_VIBRA );
        submenu.add( deployMinesVibrabomb );

        // Finish off the deploy menu.
        deployNext = new MenuItem( "Next Unit" );
        deployNext.addActionListener( this );
        deployNext.setActionCommand( DeploymentDisplay.DEPLOY_NEXT );
        menu.add( deployNext );
        deployTurn = new MenuItem( "Turn" );
        deployTurn.addActionListener( this );
        deployTurn.setActionCommand( DeploymentDisplay.DEPLOY_TURN );
        menu.add( deployTurn );
        deployLoad = new MenuItem( "Load" );
        deployLoad.addActionListener( this );
        deployLoad.setActionCommand( DeploymentDisplay.DEPLOY_LOAD );
        menu.add( deployLoad );
        deployUnload = new MenuItem( "Unload" );
        deployUnload.addActionListener( this );
        deployUnload.setActionCommand( DeploymentDisplay.DEPLOY_UNLOAD );
        menu.add( deployUnload );

        menu.addSeparator();
        
        menu.add( submenu );
        
        // *** Create the move menu.
        menu = new Menu( "Move" );
        this.add( menu );

        moveNext = new MenuItem( "Next" );
        moveNext.addActionListener( this );
        moveNext.setActionCommand( "moveNext" );
        menu.add( moveNext );
        moveWalk = new MenuItem( "Walk" );
        moveWalk.addActionListener( this );
        moveWalk.setActionCommand( "moveWalk" );
        menu.add( moveWalk );
        moveTurn = new MenuItem( "Turn" );
        moveTurn.addActionListener( this );
        moveTurn.setActionCommand( "moveTurn" );
        menu.add( moveTurn );
        moveLoad = new MenuItem( "Load" );
        moveLoad.addActionListener( this );
        moveLoad.setActionCommand( "moveLoad" );
        menu.add( moveLoad );
        moveUnload = new MenuItem( "Unload" );
        moveUnload.addActionListener( this );
        moveUnload.setActionCommand( "moveUnload" );
        menu.add( moveUnload );

        /* begin killme
        ** Remove this block when implementing the MovementDisplay.
        */
        menu.setEnabled( false );
        /* end killme */

        // *** Create the fire menu.
        menu = new Menu( "Fire" );
        this.add( menu );

        fireFire = new MenuItem( "Fire" );
        fireFire.addActionListener( this );
        fireFire.setActionCommand(FiringDisplay.FIRE_FIRE);
        menu.add( fireFire );
        fireSkip = new MenuItem( "Skip" );
        fireSkip.addActionListener( this );
        fireSkip.setActionCommand(FiringDisplay.FIRE_SKIP);
        menu.add( fireSkip );
        fireNextTarg = new MenuItem( "Next Target" );
        fireNextTarg.addActionListener( this );
        fireNextTarg.setActionCommand(FiringDisplay.FIRE_NEXT_TARG);
        menu.add( fireNextTarg );
        fireNext = new MenuItem( "Next Unit" );
        fireNext.addActionListener( this );
        fireNext.setActionCommand(FiringDisplay.FIRE_NEXT);
        menu.add( fireNext );
        menu.addSeparator();
        fireTwist = new MenuItem( "Twist" );
        fireTwist.addActionListener( this );
        fireTwist.setActionCommand(FiringDisplay.FIRE_TWIST);
        menu.add( fireTwist );
        fireFlipArms = new MenuItem( "Flip Arms" );
        fireFlipArms.addActionListener( this );
        fireFlipArms.setActionCommand(FiringDisplay.FIRE_FLIP_ARMS);
        menu.add( fireFlipArms );
        menu.addSeparator();
        fireMode = new MenuItem( "Mode" );
        fireMode.addActionListener( this );
        fireMode.setActionCommand(FiringDisplay.FIRE_MODE);
        menu.add( fireMode );

        // Create the Exclusive sub-menu.
        submenu = new Menu( "Exclusive" );
        menu.add( submenu );
        fireFindClub = new MenuItem( "Find Club" );
        fireFindClub.addActionListener( this );
        fireFindClub.setActionCommand(FiringDisplay.FIRE_FIND_CLUB);
        submenu.add( fireFindClub );
        fireSpot = new MenuItem( "Spot" );
        fireSpot.addActionListener( this );
        fireSpot.setActionCommand(FiringDisplay.FIRE_SPOT);
        submenu.add( fireSpot );

        fireCancel = new MenuItem( "Cancel" );
        fireCancel.addActionListener( this );
        fireCancel.setActionCommand(FiringDisplay.FIRE_CANCEL);
        menu.add( fireCancel );

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
        if ( this.game != null ) {
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

        // As of 2003-09-04, we can't ever print.
        filePrint.setEnabled( false );

        // If we have a board, we can still perform all board actions.
        // We can also view the mini map.
        if ( this.hasBoard ) {
            fileBoardNew.setEnabled( true );
            fileBoardOpen.setEnabled( true );
            fileBoardSave.setEnabled( true );
            fileBoardSaveAs.setEnabled( true );
            viewMiniMap.setEnabled( true );
        }
        // If we don't have a board, then we can't save it.
        // Also, we can't view the mini map.
        else {
            fileBoardNew.setEnabled( true );
            fileBoardOpen.setEnabled( true );
            fileBoardSave.setEnabled( false );
            fileBoardSaveAs.setEnabled( false );
            viewMiniMap.setEnabled( false );
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
        if ( this.phase == Game.PHASE_DEPLOY_MINEFIELDS ||
             this.phase == Game.PHASE_MOVEMENT ||
             this.phase == Game.PHASE_FIRING ||
             this.phase == Game.PHASE_PHYSICAL ||
             this.phase == Game.PHASE_DEPLOYMENT ) {
            viewLOSSetting.setEnabled( true );
            viewMiniMap.setEnabled( true );
        } else {
            viewLOSSetting.setEnabled( false );
            viewMiniMap.setEnabled( false );
        }

        // We can only view the turn report in certain phases.
        if ( this.phase == Game.PHASE_INITIATIVE ||
             this.phase == Game.PHASE_MOVEMENT ||
             this.phase == Game.PHASE_FIRING ||
             this.phase == Game.PHASE_PHYSICAL ||
             this.phase == Game.PHASE_END ||
             this.phase == Game.PHASE_DEPLOYMENT ) {
            viewTurnReport.setEnabled( true );
        } else {
            viewTurnReport.setEnabled( false );
        }

        // As of 2003-09-04, we can't ever view the initiative report.
        viewInitiativeReport.setEnabled( false );

        // As of 2003-09-04, we can always at least look at the
        // game options and client settings.
        viewGameOptions.setEnabled( true );
        viewClientSettings.setEnabled( true );

        // As of 2003-09-04, we can't ever deploy command mines.
        // If the phase is "deploy minefields", we can specify the mine type.
        if ( this.phase != Game.PHASE_DEPLOY_MINEFIELDS ) {
            deployMinesConventional.setEnabled( false );
            deployMinesCommand.setEnabled( false );
            deployMinesVibrabomb.setEnabled( false );
        } else {
	        deployMinesConventional.setLabel("Minefield(" + nbrConv + ")");
	        deployMinesCommand.setLabel("Command(" + nbrCommand + ")");
	        deployMinesVibrabomb.setLabel("Vibrabomb(" + nbrVibra + ")");
	
	        deployMinesConventional.setEnabled(nbrConv > 0);
	        deployMinesCommand.setEnabled(nbrCommand > 0);
	        deployMinesVibrabomb.setEnabled(nbrVibra > 0);
        }

        // We can only deploy units in the deployment phase.
        // Some actions require an Entity to be selected.
        if ( this.phase == Game.PHASE_DEPLOYMENT ) {
            deployNext.setEnabled( true );
            deployTurn.setEnabled( this.entity != null );
            deployLoad.setEnabled( this.entity != null );
            deployUnload.setEnabled( this.entity != null );
        } else {
            deployNext.setEnabled( false );
            deployTurn.setEnabled( false );
            deployLoad.setEnabled( false );
            deployUnload.setEnabled( false );
        }

        // We can only move units in the movement phase.
        // Some actions require an Entity to be selected.
        if ( this.phase == Game.PHASE_MOVEMENT ) {
            moveNext.setEnabled( true );
            moveWalk.setEnabled( this.entity != null );
            moveTurn.setEnabled( this.entity != null );
            moveLoad.setEnabled( this.entity != null );
            moveUnload.setEnabled( this.entity != null );
        } else { 
            moveNext.setEnabled( false );
            moveWalk.setEnabled( false );
            moveTurn.setEnabled( false );
            moveLoad.setEnabled( false );
            moveUnload.setEnabled( false );
        }

        // We can only fire selected units in the firing phase.
        if ( this.phase != Game.PHASE_FIRING || entity == null ) {
            fireFire.setEnabled( false );
            fireSkip.setEnabled( false );
            fireNextTarg.setEnabled( false );
            fireNext.setEnabled( false );
            fireTwist.setEnabled( false );
            fireFlipArms.setEnabled( false );
            fireMode.setEnabled( false );
            fireFindClub.setEnabled( false );
            fireSpot.setEnabled( false );
            fireCancel.setEnabled( false );
        } else {
            // Some actions require a visible target.
            if ( this.hasTarget ) {
                fireNextTarg.setEnabled( true );
                fireSkip.setEnabled( true );
                // Ability to fire is controlled by its own flag.
                fireFire.setEnabled( this.hasFireChoice );
            }
            fireTwist.setEnabled( entity.canChangeSecondaryFacing() );
            fireFindClub.setEnabled
                ( Compute.canMechFindClub(game, entity.getId()) );
            fireSpot.setEnabled
                ( entity.canSpot() &&
                  game.getOptions().booleanOption("indirect_fire") );
            fireFlipArms.setEnabled( entity.canFlipArms() );
            fireNext.setEnabled( true );
            fireMode.setEnabled( true );
            fireCancel.setEnabled( true );
        } // End in-firing-phase

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
        this.phase = current;
        manageMenu();
    }

    /**
     * Identify that the current <code>Entity</code> has a visible target.
     *
     * @param   available - <code>true</code> when a visible target is
     *          available.  Set this value to <code>false</code> after
     *          the target list is cleared.
     */
    public synchronized void setHasTarget( boolean available ) {
        this.hasTarget = available;
        manageMenu();
    }

    /**
     * Identify that the current <code>Entity</code> has a fire choice.
     *
     * @param   available - <code>true</code> when a fire choice is
     *          available.  Set this value to <code>false</code> after
     *          the fire choice is cleared.
     */
    public synchronized void setHasFireChoice( boolean available ) {
        this.hasFireChoice = available;
        manageMenu();
    }

	public synchronized void setNbrMinefields(int conv, 
													int command, 
													int vibra) {
		nbrConv = conv;
		nbrCommand = command;
		nbrVibra = vibra;
		manageMenu();
	}

}
