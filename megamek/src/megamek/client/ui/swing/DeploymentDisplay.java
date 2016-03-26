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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.boardview.BoardView1;
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
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Tank;
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
     * @author arlith
     *
     */
    public static enum DeployCommand implements PhaseCommand {
        DEPLOY_NEXT("deployNext"),
        DEPLOY_TURN("deployTurn"),    
        DEPLOY_LOAD("deployLoad"),
        DEPLOY_UNLOAD("deployUnload"),
        DEPLOY_REMOVE("deployRemove"),
        DEPLOY_ASSAULTDROP("assaultDrop"),
        DEPLOY_DOCK("deployDock");  
    
        public String cmd;
        
        /**
         * Priority that determines this buttons order
         */
        public int priority;
        
        private DeployCommand(String c){
            cmd = c;
        }
        
        public String getCmd(){
            return cmd;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public void setPriority(int p) {
            priority = p;
        }
        
        public String toString(){
            return Messages.getString("DeploymentDisplay." + getCmd());
        }
    }

    protected Hashtable<DeployCommand,MegamekButton> buttons;

    private int cen = Entity.NONE; // current entity number
    // is the shift key held?
    private boolean turnMode = false;
    private boolean assaultDropPreference = false;

    /**
     * Creates and lays out a new deployment phase display for the specified
     * client.
     */
    public DeploymentDisplay(ClientGUI clientgui) {      
        super(clientgui);
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);
        setupStatusBar(Messages
                .getString("DeploymentDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
        
        buttons = new Hashtable<DeployCommand, MegamekButton>(
                (int) (DeployCommand.values().length * 1.25 + 0.5));
        for (DeployCommand cmd : DeployCommand.values()) {
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
        DeployCommand commands[] = DeployCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (DeployCommand cmd : commands){
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
            disableButtons();
            setNextEnabled(true);
            System.err.println("DeploymentDisplay: " //$NON-NLS-1$
                    + "tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }
        
        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
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
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        // RACE : if player clicks fast enough, ce() is null.
        if (null != ce()) {
            setTurnEnabled(true);
            butDone.setEnabled(false);
            clientgui.getBoardView().markDeploymentHexesFor(ce());
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
            buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages
                        .getString("DeploymentDisplay.AssaultDrop")); //$NON-NLS-1$
                assaultDropPreference = false;
            }
            
            List<Entity> loadableUnits = getLoadableEntities();
            setLoadEnabled(loadableUnits.size() > 0);
            setUnloadEnabled(ce().getLoadedUnits().size() > 0);
            
            setNextEnabled(true);
            setRemoveEnabled(true);

            clientgui.mechD.displayEntity(ce());
            clientgui.mechD.showPanel("movement"); //$NON-NLS-1$

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());
        } else {
            disableButtons();
            setNextEnabled(true);
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
        disableButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        for (DeployCommand cmd : DeployCommand.values()){
            setButtonEnabled(cmd, false);
        }
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setAssaultDropEnabled(false);
    }
    
    private void setButtonEnabled(DeployCommand cmd, boolean enabled){
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

        if (ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    /**
     * Sends an entity removal to the server
     */
    private void remove() {
        disableButtons();
        clientgui.getClient().sendDeleteEntity(cen);
        cen = Entity.NONE;
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
        if (clientgui.getClient().getGame().getPhase() 
                != IGame.Phase.PHASE_DEPLOYMENT) {
            // ignore
            return;
        }
        
        if (clientgui.getClient().isMyTurn()) {
            if (cen == Entity.NONE) {
                beginMyTurn();
            }
            setStatusBarText(Messages
                    .getString("DeploymentDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            endMyTurn();
            String playerName;
            if (e.getPlayer() != null) {
                playerName = e.getPlayer().getName();
            } else {
                playerName = "Unknown";
            }
            setStatusBarText(Messages.getString(
                    "DeploymentDisplay.its_others_turn", //$NON-NLS-1$
                    new Object[] { playerName }));
        }
        
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        DeploymentDisplay.this.clientgui.bv.markDeploymentHexesFor(null);
        
       // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase() 
                == IGame.Phase.PHASE_LOUNGE) {
            endMyTurn();
        }
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
        final IBoard board = clientgui.getClient().getGame().getBoard();
        final IGame game = clientgui.getClient().getGame();
        final IHex deployhex = board.getHex(moveto);
        final Building bldg = board.getBuildingAt(moveto);
        boolean isAero = ce() instanceof Aero;
        boolean isVTOL = ce() instanceof VTOL;
        boolean isTankOnPavement = (ce() instanceof Tank)
                && (deployhex.containsTerrain(Terrains.PAVEMENT)
                        || deployhex.containsTerrain(Terrains.ROAD)
                        || deployhex.containsTerrain(Terrains.BRIDGE_ELEV));
        String title, msg;
        if ((ce().getPosition() != null) && (shiftheld || turnMode)) { // turn
            ce().setFacing(ce().getPosition().direction(moveto));
            ce().setSecondaryFacing(ce().getFacing());
            clientgui.bv.redrawEntity(ce());
            turnMode = false;
        } else if (ce().isBoardProhibited(board.getType())) {
            // check if this type of unit can be on the given type of map
            title = Messages.getString("DeploymentDisplay.alertDialog.title");  //$NON-NLS-1$
            msg = Messages.getString(
                    "DeploymentDisplay.wrongMapType",
                    new Object[] {
                            ce().getShortName(),
                            Board.getTypeName(board.getType()) }); 
            JOptionPane.showMessageDialog(clientgui, msg, title,
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (!(board.isLegalDeployment(moveto, ce().getStartingPos()) 
                    || assaultDropPreference)
                || (ce().isLocationProhibited(moveto) && !isTankOnPavement)) {
            msg = Messages.getString("DeploymentDisplay.cantDeployInto",
                    new Object[] { ce().getShortName(), moveto.getBoardNum() });
            title = Messages.getString("DeploymentDisplay.alertDialog.title"); //$NON-NLS-1$
            JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        } else if (isAero && board.inAtmosphere()
                && (ce().getElevation() <= board.getHex(moveto).ceiling(true))) {
            // Ensure aeros don't end up at lower elevation than the current hex
            title = Messages.getString("DeploymentDisplay.alertDialog.title"); //$NON-NLS-1$
            msg = Messages.getString("DeploymentDisplay.elevationTooLow",
                    new Object[] { ce().getShortName(), moveto.getBoardNum() });
            JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        } else if ((Compute.stackingViolation(game, ce().getId(), moveto) != null)
                && (bldg == null)) {
            // check if deployed unit violates stacking
            return;
        } else {
            // check for buildings and if found ask what level they want to
            // deploy at
            if ((null != bldg) && !isAero && !isVTOL) {
                if (deployhex.containsTerrain(Terrains.BLDG_ELEV)) {
                    boolean success = processBuildingDeploy(moveto);
                    if (!success) {
                        return;
                    }
                } else if (deployhex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                    boolean success = processBridgeDeploy(moveto);
                    if (!success) {
                        return;
                    }
                }
            } else if (!isAero) {
                // hovers and naval units go on the surface
                if ((ce().getMovementMode() == EntityMovementMode.NAVAL)
                        || (ce().getMovementMode() == EntityMovementMode.SUBMARINE)
                        || (ce().getMovementMode() == EntityMovementMode.HYDROFOIL)
                        || (ce().getMovementMode() == EntityMovementMode.HOVER)) {
                    ce().setElevation(0);
                } else if (isVTOL) {
                    // VTOLs go to elevation 1... unless set in the Lounge.
                    // or if mechanized BA, since VTOL movement is then illegal
                    if ((ce().getElevation() < 1) 
                            && (ce().getExternalUnits().size() <= 0)) {
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
            clientgui.bv.repaint();
            butDone.setEnabled(true);
        }
        if (!shiftheld) {
            clientgui.getBoardView().select(moveto);
        }
    }
    
    private boolean processBuildingDeploy(Coords moveto) {
        final IBoard board = clientgui.getClient().getGame().getBoard();
        final IGame game = clientgui.getClient().getGame();

        int height = board.getHex(moveto).terrainLevel(Terrains.BLDG_ELEV);
        ArrayList<String> floorNames = new ArrayList<>(height + 1);
        ArrayList<Integer> floorValues = new ArrayList<>(height + 1);

        for (int loop = 0; loop < height; loop++) {
            if (Compute.stackingViolation(game, ce(), loop, moveto, null) == null) {
                floorNames.add(Messages.getString("DeploymentDisplay.floor")
                        + Integer.toString(loop + 1));
                floorValues.add(loop);
            }
        }
        if (Compute.stackingViolation(game, ce(), height, moveto, null) == null) {
            floorNames.add(Messages.getString("DeploymentDisplay.top"));
            floorValues.add(height);
        }

        // No valid floors to deploy on
        if (floorNames.size() < 1) {
            String msg = Messages.getString("DeploymentDisplay.cantDeployInto",
                    new Object[] { ce().getShortName(), moveto.getBoardNum() });
            String title = Messages
                    .getString("DeploymentDisplay.alertDialog.title"); //$NON-NLS-1$
            JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String i18nString = "DeploymentDisplay.floorsDialog.message"; //$NON-NLS-1$;
        String msg = Messages.getString(i18nString, new Object[] { ce()
                .getShortName() });
        i18nString = "DeploymentDisplay.floorsDialog.title"; //$NON-NLS-1$
        String title = Messages.getString(i18nString);
        String input = (String) JOptionPane.showInputDialog(clientgui, msg,
                title, JOptionPane.QUESTION_MESSAGE, null,
                floorNames.toArray(), floorNames.get(0));
        if (input != null) {
            for (int i = 0; i < floorNames.size(); i++) {
                if (input.equals(floorNames.get(i))) {
                    ce().setElevation(floorValues.get(i));
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    private boolean processBridgeDeploy(Coords moveto) {
        final IBoard board = clientgui.getClient().getGame().getBoard();
        final IHex deployhex = board.getHex(moveto);

        int height = board.getHex(moveto).terrainLevel(Terrains.BRIDGE_ELEV);
        List<String> floors = new ArrayList<>(2);
        if (!ce().isLocationProhibited(moveto)) {
            floors.add(Messages.getString("DeploymentDisplay.belowbridge"));
        }
        floors.add(Messages.getString("DeploymentDisplay.topbridge"));
        
        String i18nString = "DeploymentDisplay.bridgeDialog.title"; 
        String title = Messages
                .getString(i18nString); //$NON-NLS-1$
        i18nString = "DeploymentDisplay.bridgeDialog.message"; //$NON-NLS-1$
        String msg = Messages.getString(i18nString, new Object[] { ce()
                .getShortName() });
        String input = (String) JOptionPane.showInputDialog(clientgui, msg,
                title, JOptionPane.QUESTION_MESSAGE, null, floors.toArray(), null);
        if (input != null) {
            if (input.equals(Messages.getString("DeploymentDisplay.topbridge"))) {
                ce().setElevation(height);
            } else {
                ce().setElevation(deployhex.floor() - deployhex.surface());
            }
            return true;
        } else {
            return false;
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        final Client client = clientgui.getClient();
        final String actionCmd = ev.getActionCommand();
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
        } else if (actionCmd.equals(DeployCommand.DEPLOY_NEXT.getCmd())) {
            if (ce() != null) {
                ce().setPosition(null);
                clientgui.bv.redrawEntity(ce());
                // Unload any loaded units during this turn
                List<Integer> lobbyLoadedUnits = ce().getLoadedKeepers();
                for (Entity other : ce().getLoadedUnits()) {
                    // Ignore units loaded before this turn
                    if (!lobbyLoadedUnits.contains(other.getId())) {
                        ce().unload(other);
                        other.setTransportId(Entity.NONE);
                        other.newRound(client.getGame().getRoundCount());
                    }                    
                }
            }
            selectEntity(client.getNextDeployableEntityNum(cen));
        } else if (actionCmd.equals(DeployCommand.DEPLOY_TURN.getCmd())) {
            turnMode = true;
        } else if (actionCmd.equals(DeployCommand.DEPLOY_LOAD.getCmd())) {
            // What undeployed units can we load?
            List<Entity> choices = getLoadableEntities();

            // Do we have anyone to load?
            if (choices.size() > 0) {
                String input = (String) JOptionPane
                        .showInputDialog(
                                clientgui,
                                Messages.getString(
                                        "DeploymentDisplay.loadUnitDialog.message",
                                        new Object[] { ce().getShortName(),
                                                ce().getUnusedString() }), //$NON-NLS-1$
                                Messages.getString("DeploymentDisplay.loadUnitDialog.title"), //$NON-NLS-1$
                                JOptionPane.QUESTION_MESSAGE, null,
                                SharedUtility.getDisplayArray(choices), null);
                Entity other = (Entity) SharedUtility.getTargetPicked(choices, input);
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
                        String title = Messages.getString("DeploymentDisplay." + //$NON-NLS-1$
                                "loadUnitBayNumberDialog.title"); //$NON-NLS-1$
                        String msg = Messages.getString("DeploymentDisplay." + //$NON-NLS-1$
                                "loadUnitBayNumberDialog.message", //$NON-NLS-1$
                                new Object[] { ce().getShortName() });
                        String bayString = (String) JOptionPane
                                .showInputDialog(clientgui, msg, title,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        retVal, null);
                        int bayNum = Integer.parseInt(bayString.substring(0,
                                bayString.indexOf(" ")));
                        other.setTargetBay(bayNum);
                        // We need to update the entity here so that the server
                        // knows about our target bay
                        client.sendUpdateEntity(other);
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
                    setUnloadEnabled(true);
                }
            } else { // End have-choices
                JOptionPane.showMessageDialog(clientgui.frame, Messages
                        .getString("DeploymentDisplay.alertDialog1.message",
                                new Object[] { ce().getShortName() }), Messages
                        .getString("DeploymentDisplay.alertDialog1.title") //$NON-NLS-1$
                        , JOptionPane.ERROR_MESSAGE);
            }
        } // End load-unit
        else if (actionCmd.equals(DeployCommand.DEPLOY_UNLOAD.getCmd())) {
            // Do we have anyone to unload?
            Entity loader = ce();
            List<Entity> choices = loader.getLoadedUnits();
            if (choices.size() > 0) {
                Entity loaded = null;
                String msg = Messages.getString("DeploymentDisplay." //$NON-NLS-1$
                        + "unloadUnitDialog.message", //$NON-NLS-1$
                        new Object[] { ce().getShortName(),
                                ce().getUnusedString() });
                String title = Messages.getString("DeploymentDisplay." + //$NON-NLS-1$
                        "unloadUnitDialog.title"); //$NON-NLS-1$
                String input = (String) JOptionPane.showInputDialog(clientgui,
                        msg, title, JOptionPane.QUESTION_MESSAGE, null,
                        SharedUtility.getDisplayArray(choices), null);
                loaded = (Entity) SharedUtility.getTargetPicked(choices, input);
                if (loaded != null) {
                    if (loader.unload(loaded)) {
                        loaded.setTransportId(Entity.NONE);
                        loaded.newRound(clientgui.getClient().getGame()
                                .getRoundCount());
                        clientgui.mechD.displayEntity(ce());
                        // Unit loaded in the lobby?  Server needs updating
                        if (loader.getLoadedKeepers().contains(loaded.getId())) {
                            Vector<Integer> lobbyLoaded = loader.getLoadedKeepers();
                            lobbyLoaded.removeElement(loaded.getId());
                            loader.setLoadedKeepers(lobbyLoaded);
                            client.sendDeploymentUnload(loader, loaded);
                            // Need to take turn for unloaded unit, so select it
                            selectEntity(loaded.getId());
                        }
                        setLoadEnabled(getLoadableEntities().size() > 0);
                    } else {
                        System.out.println("Could not unload " + //$NON-NLS-1$
                                loaded.getShortName()
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
        else if (actionCmd.equals(DeployCommand.DEPLOY_REMOVE.getCmd())) {
            if (JOptionPane.showConfirmDialog(clientgui.frame, Messages
                    .getString("DeploymentDisplay.removeUnit",
                            new Object[] { ce().getShortName() }), Messages
                    .getString("DeploymentDisplay.removeTitle"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                remove();
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_ASSAULTDROP.getCmd())) {
            assaultDropPreference = !assaultDropPreference;
            if (assaultDropPreference) {
            buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages
                        .getString("DeploymentDisplay.assaultDropOff"));
            } else {
            buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages
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
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()) {
            clientgui.setDisplayVisible(true);
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
        if (client.isMyTurn()) {
            if (client.getGame().getTurn()
                    .isValidEntity(e, client.getGame())) {
                if (ce() != null) {
                    ce().setPosition(null);
                    clientgui.bv.redrawEntity(ce());
                    // Unload any loaded units during this turn
                    List<Integer> lobbyLoadedUnits = ce().getLoadedKeepers();
                    for (Entity other : ce().getLoadedUnits()) {
                        // Ignore units loaded before this turn
                        if (!lobbyLoadedUnits.contains(other.getId())) {
                            ce().unload(other);
                            other.setTransportId(Entity.NONE);
                            other.newRound(client.getGame().getRoundCount());
                        }                    
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
        buttons.get(DeployCommand.DEPLOY_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }

    private void setTurnEnabled(boolean enabled) {
    buttons.get(DeployCommand.DEPLOY_TURN).setEnabled(enabled);
        clientgui.getMenuBar().setDeployTurnEnabled(enabled);
    }

    private void setLoadEnabled(boolean enabled) {
    buttons.get(DeployCommand.DEPLOY_LOAD).setEnabled(enabled);
        clientgui.getMenuBar().setDeployLoadEnabled(enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
    buttons.get(DeployCommand.DEPLOY_UNLOAD).setEnabled(enabled);
        clientgui.getMenuBar().setDeployUnloadEnabled(enabled);
    }

    private void setRemoveEnabled(boolean enabled) {
    buttons.get(DeployCommand.DEPLOY_REMOVE).setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }

    private void setAssaultDropEnabled(boolean enabled) {
    buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setEnabled(enabled);
        clientgui.getMenuBar().setDeployAssaultDropEnabled(enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        die();
    }
    
    /**
     * Returns a list of the entities that can be loaded into the currently
     * selected entity.
     * 
     * @return
     */
    private List<Entity> getLoadableEntities() {       
        ArrayList<Entity> choices = new ArrayList<Entity>();
        // If current entity is null, nothing to do
        if (ce() == null) {
            return choices;
        }
        List<Entity> entities = clientgui.getClient().getGame()
                .getEntitiesVector();
        for (Entity other : entities) {        
            if (other.isSelectableThisTurn() && ce().canLoad(other, false)) {
                choices.add(other);
            }
        }
        return choices;
    }
}
