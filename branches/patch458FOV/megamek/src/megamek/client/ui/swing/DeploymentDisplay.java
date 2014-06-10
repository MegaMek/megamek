/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.Board;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Terrains;
import megamek.common.Transporter;
import megamek.common.VTOL;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class DeploymentDisplay extends StatusBarPhaseDisplay {
    /**
     *
     */
    private static final long serialVersionUID = -430925219438520710L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the deployment phase.  Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
     * @author walczak
     *
     */
    public static enum Command {
	    DEPLOY_NEXT("deployNext"),
	    DEPLOY_TURN("deployTurn"),    
	    DEPLOY_LOAD("deployLoad"),
	    DEPLOY_UNLOAD("deployUnload"),
	    DEPLOY_REMOVE("deployRemove"),
	    DEPLOY_ASSAULTDROP("assaultDrop"),
	    DEPLOY_DOCK("deployDock");  
    
	    String cmd;
	    private Command(String c){
	    	cmd = c;
	    }
	    
	    public String getCmd(){
	    	return cmd;
	    }
	    
	    public String toString(){
	    	return cmd;
	    }
    }

    protected Hashtable<Command,MegamekButton> buttons;

    private int cen = Entity.NONE; // current entity number
    // is the shift key held?
    private boolean turnMode = false;
    private boolean assaultDropPreference = false;

    /**
     * Creates and lays out a new deployment phase display for the specified
     * client.
     */
    public DeploymentDisplay(ClientGUI clientgui) {      
        this.clientgui = clientgui;
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);
        setupStatusBar(Messages
                .getString("DeploymentDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
        
		buttons = new Hashtable<Command, MegamekButton>(
				(int) (Command.values().length * 1.25 + 0.5));
		for (Command cmd : Command.values()) {
			String title = Messages.getString("DeploymentDisplay."
					+ cmd.getCmd());
			MegamekButton newButton = new MegamekButton(title, "PhaseDisplayButton");
			newButton.addActionListener(this);
			newButton.setActionCommand(cmd.getCmd());
			newButton.setEnabled(false);
			buttons.put(cmd, newButton);
		}  		
		numButtonGroups = 
        		(int)Math.ceil((buttons.size()+0.0) / buttonsPerGroup);

        butDone.setText("<html><b>" + Messages.getString("DeploymentDisplay.Deploy") + "</b></html>"); //$NON-NLS-1$
        butDone.setEnabled(false);

        
        layoutScreen();
        
        setupButtonPanel();        
    }
    
    protected ArrayList<MegamekButton> getButtonList(){                
        ArrayList<MegamekButton> buttonList = new ArrayList<MegamekButton>();        
        for (Command cmd : Command.values()){
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Selects an entity for deployment
     */
    public void selectEntity(int en) {
        // hmm, sometimes this gets called when there's no ready entities?
        if (clientgui.getClient().getGame().getEntity(en) == null) {
            System.err
                    .println("DeploymentDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }

        // FIXME: Hack alert: remove C3 sprites from earlier here, or we might
        // crash when
        // trying to draw a c3 sprite belonging to the previously selected,
        // but not deployed entity. BoardView1 should take care of that itself.
        if (clientgui.bv instanceof BoardView1) {
            ((BoardView1) clientgui.bv).clearC3Networks();
        }
        cen = en;
        clientgui.setSelectedEntityNum(en);
        setTurnEnabled(true);
        butDone.setEnabled(false);
        setLoadEnabled(true);
        setUnloadEnabled(true);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().markDeploymentHexesFor(ce());
        // RACE : if player clicks fast enough, ce() is null.
        if (null != ce()) {
            // set facing according to starting position
            switch (ce().getStartingPos()) {
                case Board.START_W:
                    ce().setFacing(1);
                    ce().setSecondaryFacing(1);
                    break;
                case Board.START_SW:
                    ce().setFacing(1);
                    ce().setSecondaryFacing(1);
                    break;
                case Board.START_S:
                    ce().setFacing(0);
                    ce().setSecondaryFacing(0);
                    break;
                case Board.START_SE:
                    ce().setFacing(5);
                    ce().setSecondaryFacing(5);
                    break;
                case Board.START_E:
                    ce().setFacing(5);
                    ce().setSecondaryFacing(5);
                    break;
                case Board.START_NE:
                    ce().setFacing(4);
                    ce().setSecondaryFacing(4);
                    break;
                case Board.START_N:
                    ce().setFacing(3);
                    ce().setSecondaryFacing(3);
                    break;
                case Board.START_NW:
                    ce().setFacing(2);
                    ce().setSecondaryFacing(2);
                    break;
                case Board.START_ANY:
                    ce().setFacing(0);
                    ce().setSecondaryFacing(0);
                    break;
            }
            setAssaultDropEnabled(ce().canAssaultDrop()
                    && ce().getGame().getOptions()
                            .booleanOption("assault_drop"));
            if (!ce().canAssaultDrop()
                    && ce().getGame().getOptions()
                            .booleanOption("assault_drop")) {
            buttons.get(Command.DEPLOY_ASSAULTDROP).setText(Messages
                        .getString("DeploymentDisplay.AssaultDropOn")); //$NON-NLS-1$
                assaultDropPreference = false;
            }

            clientgui.mechD.displayEntity(ce());
            clientgui.mechD.showPanel("movement"); //$NON-NLS-1$

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());
        }
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        clientgui.setDisplayVisible(true);
        selectEntity(clientgui.getClient().getFirstDeployableEntityNum());
        setNextEnabled(true);
        setRemoveEnabled(true);
        // mark deployment hexes
        clientgui.bv.markDeploymentHexesFor(ce());
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if ((IGame.Phase.PHASE_DEPLOYMENT == clientgui.getClient().getGame()
                .getPhase())
                && (null != next)
                && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.markDeploymentHexesFor(null);
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        for (Command cmd : Command.values()){
        setButtonEnabled(cmd, false);
        }
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setAssaultDropEnabled(false);
    }
    
    private void setButtonEnabled(Command cmd, boolean enabled){
    MegamekButton button = buttons.get(cmd);
    if (button != null){
    button.setEnabled(enabled);
    }
    }

    /**
     * Sends a deployment to the server
     */
    @Override
    public void ready() {
        Entity en = ce();

        if ((en instanceof Dropship) && !en.isAirborne()) {
            ArrayList<Coords> crushedBuildingLocs = new ArrayList<Coords>();
            ArrayList<Coords> secondaryPositions = new ArrayList<Coords>();
            secondaryPositions.add(en.getPosition());
            for (int dir = 0; dir < 6; dir++) {
                secondaryPositions.add(en.getPosition().translated(dir));
            }
            for (Coords pos : secondaryPositions) {
                Building bld = clientgui.getClient().getGame().getBoard()
                        .getBuildingAt(pos);
                if (bld != null) {
                    crushedBuildingLocs.add(pos);
                }
            }
            if (!crushedBuildingLocs.isEmpty()) {
                JOptionPane
                        .showMessageDialog(
                                clientgui,
                                Messages.getString("DeploymentDisplay.dropshipBuildingDeploy"), //$NON-NLS-1$
                                Messages.getString("DeploymentDisplay.alertDialog.title"), //$NON-NLS-1$
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        disableButtons();

        clientgui.getClient().deploy(cen, en.getPosition(), en.getFacing(),
                en.getElevation(), en.getLoadedUnits(), assaultDropPreference);
        en.setDeployed(true);
    }

    /**
     * Sends an entity removal to the server
     */
    private void remove() {
        disableButtons();
        clientgui.getClient().sendDeleteEntity(cen);
        beginMyTurn();
    }

    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return clientgui.getClient().getGame().getEntity(cen);
    }

    public void die() {
        if (clientgui.getClient().isMyTurn()) {
            endMyTurn();
        }
        clientgui.bv.markDeploymentHexesFor(null);
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        removeAll();
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (clientgui.getClient().isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages
                    .getString("DeploymentDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            endMyTurn();
            setStatusBarText(Messages
                    .getString(
                            "DeploymentDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        DeploymentDisplay.this.clientgui.bv.markDeploymentHexesFor(null);
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_DEPLOYMENT) {
            setStatusBarText(Messages
                    .getString("DeploymentDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
        }
    }

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (b.getType() != BoardViewEvent.BOARD_HEX_DRAGGED) {
            return;
        }

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn() || (ce() == null)
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
        boolean shiftheld = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;

        // check for a deployment
        Coords moveto = b.getCoords();
        if ((ce().getPosition() != null) && (shiftheld || turnMode)) { // turn
            ce().setFacing(ce().getPosition().direction(moveto));
            ce().setSecondaryFacing(ce().getFacing());
            clientgui.bv.redrawEntity(ce());
            turnMode = false;
        } else if (ce().isBoardProhibited(
                clientgui.getClient().getGame().getBoard().getType())) {
            // check if this type of unit can be on the given type of map
            JOptionPane
                    .showMessageDialog(
                            clientgui,
                            Messages.getString("DeploymentDisplay.alertDialog.title"), //$NON-NLS-1$
                            Messages.getString(
                                    "DeploymentDisplay.wrongMapType", new Object[] { ce().getShortName(), Board.getTypeName(clientgui.getClient().getGame().getBoard().getType()) }), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
            return;
        } else if (!(clientgui.getClient().getGame().getBoard()
                .isLegalDeployment(moveto, ce().getStartingPos()) || assaultDropPreference)
                || ce().isLocationProhibited(moveto)) {
            JOptionPane
                    .showMessageDialog(
                            clientgui.frame,
                            Messages.getString(
                                    "DeploymentDisplay.cantDeployInto", new Object[] { ce().getShortName(), moveto.getBoardNum() }), Messages.getString("DeploymentDisplay.alertDialog.title") //$NON-NLS-1$
                            , JOptionPane.ERROR_MESSAGE);
            return;
        } else if ((ce() instanceof Aero)
                && clientgui.getClient().getGame().getBoard().inAtmosphere()
                && (ce().getElevation() <= clientgui.getClient().getGame()
                        .getBoard().getHex(moveto).ceiling())) {
            // make sure aeros don't end up at a lower elevation than the
            // current hex
            JOptionPane
                    .showMessageDialog(
                            clientgui.frame,
                            Messages.getString(
                                    "DeploymentDisplay.elevationTooLow", new Object[] { ce().getShortName(), moveto.getBoardNum() }), Messages.getString("DeploymentDisplay.alertDialog.title") //$NON-NLS-1$
                            , JOptionPane.ERROR_MESSAGE);
            return;
        } else if (Compute.stackingViolation(clientgui.getClient().getGame(),
                ce().getId(), moveto) != null) {
            // check if deployed unit violates stacking
            return;
        } else {
            // check for buildings and if found ask what level they want to
            // deploy at
            Building bldg = clientgui.getClient().getGame().getBoard()
                    .getBuildingAt(moveto);
            if ((null != bldg) && !(ce() instanceof Aero)) {
                if (clientgui.getClient().getGame().getBoard().getHex(moveto)
                        .containsTerrain(Terrains.BLDG_ELEV)) {
                    int height = clientgui.getClient().getGame().getBoard()
                            .getHex(moveto).terrainLevel(Terrains.BLDG_ELEV);
                    String[] floors = new String[height + 1];
                    for (int loop = 1; loop <= height; loop++) {
                        floors[loop - 1] = Messages
                                .getString("DeploymentDisplay.floor")
                                + Integer.toString(loop);
                    }
                    floors[height] = Messages
                            .getString("DeploymentDisplay.top");
                    String input = (String) JOptionPane
                            .showInputDialog(
                                    clientgui,
                                    Messages.getString(
                                            "DeploymentDisplay.floorsDialog.message", new Object[] { ce().getShortName() }), //$NON-NLS-1$
                                    Messages.getString("DeploymentDisplay.floorsDialog.title"), //$NON-NLS-1$
                                    JOptionPane.QUESTION_MESSAGE, null, floors,
                                    null);
                    if (input != null) {
                        for (int loop = 0; loop < floors.length; loop++) {
                            if (input.equals(floors[loop])) {
                                ce().setElevation(loop);
                                break;
                            }
                        }
                    } else {
                        ce().setElevation(0);
                    }
                } else if (clientgui.getClient().getGame().getBoard()
                        .getHex(moveto).containsTerrain(Terrains.BRIDGE_ELEV)) {
                    int height = clientgui.getClient().getGame().getBoard()
                            .getHex(moveto).terrainLevel(Terrains.BRIDGE_ELEV);
                    String[] floors = new String[2];
                    floors[0] = Messages
                            .getString("DeploymentDisplay.belowbridge");
                    floors[1] = Messages
                            .getString("DeploymentDisplay.topbridge");
                    String input = (String) JOptionPane
                            .showInputDialog(
                                    clientgui,
                                    Messages.getString(
                                            "DeploymentDisplay.bridgeDialog.message", new Object[] { ce().getShortName() }), //$NON-NLS-1$
                                    Messages.getString("DeploymentDisplay.bridgeDialog.title"), //$NON-NLS-1$
                                    JOptionPane.QUESTION_MESSAGE, null, floors,
                                    null);
                    if (input != null) {
                        if (input.equals(floors[1])) {
                            ce().setElevation(height);
                        } else {
                            IHex deployhex = clientgui.getClient().getGame()
                                    .getBoard().getHex(moveto);
                            ce().setElevation(
                                    deployhex.floor() - deployhex.surface());
                        }
                    } else {
                        IHex deployhex = clientgui.getClient().getGame()
                                .getBoard().getHex(moveto);
                        ce().setElevation(
                                deployhex.floor() - deployhex.surface());
                    }
                }
            } else if (!(ce() instanceof Aero)) {
                IHex deployhex = clientgui.getClient().getGame().getBoard()
                        .getHex(moveto);
                // hovers and naval units go on the surface
                if ((ce().getMovementMode() == EntityMovementMode.NAVAL)
                        || (ce().getMovementMode() == EntityMovementMode.SUBMARINE)
                        || (ce().getMovementMode() == EntityMovementMode.HYDROFOIL)
                        || (ce().getMovementMode() == EntityMovementMode.HOVER)) {
                    ce().setElevation(0);
                } else if (ce() instanceof VTOL) {
                    // VTOLs go to elevation 1... unless they were set in the
                    // Lounge.
                    if (ce().getElevation() < 1) {
                        ce().setElevation(1);
                    }
                } else {
                    // everything else goes to elevation 0, or on the floor of a
                    // water hex
                    ce().setElevation(deployhex.floor() - deployhex.surface());
                }
            }
            ce().setPosition(moveto);

            clientgui.bv.redrawEntity(ce());
            butDone.setEnabled(true);
        }
        if (!shiftheld) {
            clientgui.getBoardView().select(moveto);
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
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
        } else if (ev.getActionCommand().equals(Command.DEPLOY_NEXT.getCmd())) {
            if (ce() != null) {
                ce().setPosition(null);
                clientgui.bv.redrawEntity(ce());
                // Unload any loaded units.
                for (Entity other : ce().getLoadedUnits()) {
                    // Please note, the Server never got this unit's load
                    // orders.
                    ce().unload(other);
                    other.setTransportId(Entity.NONE);
                    other.newRound(clientgui.getClient().getGame()
                            .getRoundCount());
                }
                // if any of these were loaded in the chat lounge I need to
                // reload them however
                for (int otherId : ce().getLoadedKeepers()) {
                    ce().load(ce().getGame().getEntity(otherId));
                    ce().getGame().getEntity(otherId)
                            .setTransportId(ce().getId());
                }
            }
            selectEntity(clientgui.getClient().getNextDeployableEntityNum(cen));
        } else if (ev.getActionCommand().equals(Command.DEPLOY_TURN.getCmd())) {
            turnMode = true;
        } else if (ev.getActionCommand().equals(Command.DEPLOY_LOAD.getCmd())) {
            // What undeployed units can we load?
            Vector<Entity> choices = new Vector<Entity>();
            Enumeration<Entity> entities = clientgui.getClient().getGame()
                    .getEntities();
            Entity other;
            while (entities.hasMoreElements()) {
                other = entities.nextElement();
                if (other.isSelectableThisTurn() && (ce() != null)
                        && ce().canLoad(other, false)) {
                    choices.addElement(other);
                }
            }

            // Do we have anyone to load?
            if (choices.size() > 0) {
                String input = (String) JOptionPane
                        .showInputDialog(
                                clientgui,
                                Messages.getString(
                                        "DeploymentDisplay.loadUnitDialog.message", new Object[] { ce().getShortName(), ce().getUnusedString() }), //$NON-NLS-1$
                                Messages.getString("DeploymentDisplay.loadUnitDialog.title"), //$NON-NLS-1$
                                JOptionPane.QUESTION_MESSAGE, null,
                                SharedUtility.getDisplayArray(choices), null);
                other = (Entity) SharedUtility.getTargetPicked(choices, input);
                if (!(other instanceof Infantry)) {
                    Vector<Integer> bayChoices = new Vector<Integer>();
                    for (Transporter t : ce().getTransports()) {
                        if (t.canLoad(other) && (t instanceof Bay)) {
                            bayChoices.add(((Bay) t).getBayNumber());
                        }
                    }
                    String[] retVal = new String[bayChoices.size()];
                    int i = 0;
                    for (Integer bn : bayChoices) {
                        retVal[i++] = bn.toString() + " (Free Slots: "
                                + (int) ce().getBayById(bn).getUnused() + ")";
                    }
                    if ((bayChoices.size() > 1) && !(other instanceof Infantry)) {
                        String bayString = (String) JOptionPane
                                .showInputDialog(
                                        clientgui,
                                        Messages.getString(
                                                "DeploymentDisplay.loadUnitBayNumberDialog.message", new Object[] { ce().getShortName() }), //$NON-NLS-1$
                                        Messages.getString("DeploymentDisplay.loadUnitBayNumberDialog.title"), //$NON-NLS-1$
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        retVal, null);
                        int bayNum = Integer.parseInt(bayString.substring(0,
                                bayString.indexOf(" ")));
                        other.setTargetBay(bayNum);
                        // We need to update the entity here so that the server
                        // knows about our target bay
                        clientgui.getClient().sendUpdateEntity(other);
                    } else if (other != null) {
                        other.setTargetBay(-1); // Safety set!
                    }
                } else if (other != null) {
                    other.setTargetBay(-1); // Safety set!
                }
                if (other != null) {
                    // Please note, the Server may never get this load order.
                    ce().load(other, false, other.getTargetBay());
                    other.setTransportId(cen);
                    clientgui.mechD.displayEntity(ce());
                }
            } // End have-choices
            else {
                JOptionPane
                        .showMessageDialog(
                                clientgui.frame,
                                Messages.getString(
                                        "DeploymentDisplay.alertDialog1.message", new Object[] { ce().getShortName() }), Messages.getString("DeploymentDisplay.alertDialog1.title") //$NON-NLS-1$
                                , JOptionPane.ERROR_MESSAGE);
            }
        } // End load-unit
        else if (ev.getActionCommand().equals(Command.DEPLOY_UNLOAD.getCmd())) {
            // Do we have anyone to unload?
            List<Entity> choices = ce().getLoadedUnits();
            if (choices.size() > 0) {
                Entity other = null;
                String input = (String) JOptionPane
                        .showInputDialog(
                                clientgui,
                                Messages.getString(
                                        "DeploymentDisplay.unloadUnitDialog.message", new Object[] { ce().getShortName(), ce().getUnusedString() }), //$NON-NLS-1$
                                Messages.getString("DeploymentDisplay.unloadUnitDialog.title"), //$NON-NLS-1$
                                JOptionPane.QUESTION_MESSAGE, null,
                                SharedUtility.getDisplayArray(choices), null);
                other = (Entity) SharedUtility.getTargetPicked(choices, input);
                if (other != null) {
                    // Please note, the Server never got this load order.
                    if (ce().unload(other)) {
                        other.setTransportId(Entity.NONE);
                        other.newRound(clientgui.getClient().getGame()
                                .getRoundCount());
                        clientgui.mechD.displayEntity(ce());
                    } else {
                        System.out.println("Could not unload " + //$NON-NLS-1$
                                other.getShortName()
                                + " from " + ce().getShortName()); //$NON-NLS-1$
                    }
                }
            } // End have-choices
            else {
                JOptionPane
                        .showMessageDialog(
                                clientgui.frame,
                                Messages.getString(
                                        "DeploymentDisplay.alertDialog2.message", new Object[] { ce().getShortName() }), Messages.getString("DeploymentDisplay.alertDialog2.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            }
        } // End unload-unit
        else if (ev.getActionCommand().equals(Command.DEPLOY_REMOVE.getCmd())) {
            if (JOptionPane.showConfirmDialog(clientgui.frame, Messages
                    .getString("DeploymentDisplay.removeTitle"), Messages
                    .getString("DeploymentDisplay.removeMessage",
                            new Object[] { ce().getShortName() }),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                remove();
            }
        } else if (ev.getActionCommand().equals(Command.DEPLOY_ASSAULTDROP.getCmd())) {
            assaultDropPreference = !assaultDropPreference;
            if (assaultDropPreference) {
            buttons.get(Command.DEPLOY_ASSAULTDROP).setText(Messages
                        .getString("DeploymentDisplay.assaultDropOff"));
            } else {
            buttons.get(Command.DEPLOY_ASSAULTDROP).setText(Messages
                        .getString("DeploymentDisplay.assaultDrop"));
            }
        }
    } // End public void actionPerformed(ActionEvent ev)

    @Override
    public void clear() {
        beginMyTurn();
    }

    //
    // BoardViewListener
    //
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // ignore
    }

    // Selected a unit in the unit overview.
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
            if (clientgui.getClient().getGame().getTurn()
                    .isValidEntity(e, clientgui.getClient().getGame())) {
                if (ce() != null) {
                    ce().setPosition(null);
                    clientgui.bv.redrawEntity(ce());
                    // Unload any loaded units.
                    for (Entity other : ce().getLoadedUnits()) {

                        // Please note, the Server never got this unit's load
                        // orders.
                        ce().unload(other);
                        other.setTransportId(Entity.NONE);
                        other.newRound(clientgui.getClient().getGame()
                                .getRoundCount());
                    }
                }
                selectEntity(e.getId());
                if (null != e.getPosition()) {
                    clientgui.bv.centerOnHex(e.getPosition());
                }
            }
        } else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
                clientgui.bv.centerOnHex(e.getPosition());
            }
        }
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(Command.DEPLOY_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }

    private void setTurnEnabled(boolean enabled) {
    buttons.get(Command.DEPLOY_TURN).setEnabled(enabled);
        clientgui.getMenuBar().setDeployTurnEnabled(enabled);
    }

    private void setLoadEnabled(boolean enabled) {
    buttons.get(Command.DEPLOY_LOAD).setEnabled(enabled);
        clientgui.getMenuBar().setDeployLoadEnabled(enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
    buttons.get(Command.DEPLOY_UNLOAD).setEnabled(enabled);
        clientgui.getMenuBar().setDeployUnloadEnabled(enabled);
    }

    private void setRemoveEnabled(boolean enabled) {
    buttons.get(Command.DEPLOY_REMOVE).setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }

    private void setAssaultDropEnabled(boolean enabled) {
    buttons.get(Command.DEPLOY_ASSAULTDROP).setEnabled(enabled);
        clientgui.getMenuBar().setDeployAssaultDropEnabled(enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        die();
    }
}
