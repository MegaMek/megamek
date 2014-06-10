/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
import java.util.Hashtable;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.SpecialHexDisplay;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class SelectArtyAutoHitHexDisplay extends StatusBarPhaseDisplay {

    private static final long serialVersionUID = -4948184589134809323L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the select arty auto hit phase.  Each command has a 
     * string for the command plus a flag that determines what unit type it is 
     * appropriate for.
     * @author walczak
     *
     */
    public static enum Command {
    	SET_HIT_HEX("setAutoHitHex");
    
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
    
    // buttons
    protected Hashtable<Command,MegamekButton> buttons;

    private IPlayer p;
    private PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();
    
    private int startingHexes;

    /**
     * Creates and lays out a new select designated hex phase display for the specified
     * clientgui.getClient().
     */
    public SelectArtyAutoHitHexDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        clientgui.getClient().getGame().addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages
                .getString("SelectArtyAutoHitHexDisplay.waitingArtillery")); //$NON-NLS-1$

        p = clientgui.getClient().getLocalPlayer();

        artyAutoHitHexes.setPlayerID(p.getId());

        buttons = new Hashtable<Command, MegamekButton>(
				(int) (Command.values().length * 1.25 + 0.5));
		for (Command cmd : Command.values()) {
			String title = Messages.getString("SelectArtyAutoHitHexDisplay."
					+ cmd.getCmd());
			MegamekButton newButton = new MegamekButton(title, "PhaseDisplayButton");
			newButton.addActionListener(this);
			newButton.setActionCommand(cmd.getCmd());
			newButton.setEnabled(false);
			buttons.put(cmd, newButton);
		}  		
		numButtonGroups = 
        		(int)Math.ceil((buttons.size()+0.0) / buttonsPerGroup);
		
		butDone.setText(Messages
                .getString("SelectArtyAutoHitHexDisplay.Done")); //$NON-NLS-1$
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
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        // Make sure we've got the correct local player
        p = clientgui.getClient().getLocalPlayer();
        // By default, we should get 5 hexes per 4 mapsheets
        // 4 mapsheets is 16*17*4 hexes, so 1088        
        IGame game = clientgui.getClient().getGame();
        IBoard board = game.getBoard();
        int preDesignateArea = 
                game.getOptions().intOption("map_area_predesignate");
        int hexesPer = 
                game.getOptions().intOption("num_hexes_predesignate");
        double mapArea = board.getWidth() * board.getHeight();
        startingHexes = (int) Math.ceil((mapArea)/preDesignateArea)*hexesPer;
        artyAutoHitHexes.clear();
        setArtyEnabled(startingHexes);
        butDone.setEnabled(true);
    }

    /**
     * Clears out old data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setArtyEnabled(0);

        butDone.setEnabled(false);
    }

    private void addArtyAutoHitHex(Coords coords) {
        if (!clientgui.getClient().getGame().getBoard().contains(coords)) {
            return;
        }
        if (!artyAutoHitHexes.contains(coords)
                && (artyAutoHitHexes.size() < startingHexes)
                && clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.title"), //$NON-NLS-1$
                                Messages
                                        .getString(
                                                "SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.message", new Object[] { coords.getBoardNum() }))) { //$NON-NLS-1$
            artyAutoHitHexes.addElement(coords);
            setArtyEnabled(startingHexes - artyAutoHitHexes.size());
            p.addArtyAutoHitHex(coords);
            clientgui
                    .getClient()
                    .getGame()
                    .getBoard()
                    .addSpecialHexDisplay(
                            coords,
                            new SpecialHexDisplay(
                                    SpecialHexDisplay.Type.ARTILLERY_AUTOHIT,
                                    SpecialHexDisplay.NO_ROUND, p,
                                    "Artilery autohit, for player "
                                            + p.getName(),
                                    SpecialHexDisplay.SHD_OBSCURED_TEAM));
            clientgui.getBoardView().refreshDisplayables();
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
        if (!clientgui.getClient().isMyTurn()
                || ((b.getModifiers() & InputEvent.BUTTON1_MASK) == 0)) {
            return;
        }

        // check for a deployment
        clientgui.getBoardView().select(b.getCoords());
        addArtyAutoHitHex(b.getCoords());
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

        endMyTurn();

        if (clientgui.getClient().isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages
                    .getString("SelectArtyAutoHitHexDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            setStatusBarText(Messages
                    .getString(
                            "SelectArtyAutoHitHexDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
        }
    }

    /**
     * called when the game changes phase.
     *
     * @param e
     *            ignored parameter
     */
    @Override
    public void gamePhaseChange(final GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && (clientgui.getClient().getGame().getPhase() != IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES)) {
            endMyTurn();
        }
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
            setStatusBarText(Messages
                    .getString("SelectArtyAutoHitHexDisplay.waitingMinefieldPhase")); //$NON-NLS-1$
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
        }
    } // End public void actionPerformed(ActionEvent ev)

    @Override
    public void clear() {
        artyAutoHitHexes.clear();
        p.removeArtyAutoHitHexes();
        setArtyEnabled(startingHexes);
    }

    @Override
    public void ready() {
        endMyTurn();
        clientgui.getClient().sendArtyAutoHitHexes(artyAutoHitHexes);
        clientgui.getClient().sendPlayerInfo();
    }

    private void setArtyEnabled(int nbr) {
        buttons.get(Command.SET_HIT_HEX).setText(Messages.getString(
        		"SelectArtyAutoHitHexDisplay." +Command.SET_HIT_HEX.getCmd(), 
        		new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        buttons.get(Command.SET_HIT_HEX).setEnabled(nbr > 0);
        // clientgui.getMenuBar().setSelectArtyAutoHitHexEnabled(nbr);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

}
