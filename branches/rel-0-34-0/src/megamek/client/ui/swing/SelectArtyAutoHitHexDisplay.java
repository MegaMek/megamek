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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class SelectArtyAutoHitHexDisplay extends StatusBarPhaseDisplay
        implements DoneButtoned, KeyListener {

    private static final long serialVersionUID = -4948184589134809323L;

    // parent game
    public ClientGUI clientgui;
    private Client client;

    public static final String SET_HIT_HEX = "setAutoHitHex"; //$NON-NLS-1$

    // buttons
    private JPanel panButtons;

    private JButton butA;
    private JButton butDone;

    private Player p;
    private PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();

    /**
     * Creates and lays out a new deployment phase display for the specified
     * client.
     */
    public SelectArtyAutoHitHexDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        client = clientgui.getClient();
        client.game.addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages
                .getString("SelectArtyAutoHitHexDisplay.waitingArtillery")); //$NON-NLS-1$

        p = client.getLocalPlayer();

        artyAutoHitHexes.setPlayerID(p.getId());

        butA = new JButton(Messages
                .getString("SelectArtyAutoHitHexDisplay.artilleryAutohithexes")); //$NON-NLS-1$
        butA.addActionListener(this);
        butA.setActionCommand(SET_HIT_HEX);
        butA.setEnabled(false);

        butDone = new JButton(Messages
                .getString("SelectArtyAutoHitHexDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(0, 2));
        panButtons.add(butA);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);

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

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setArtyEnabled(5);
        clientgui.bv.markDeploymentHexesFor(p);
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
        if (!client.game.getBoard().contains(coords)) {
            return;
        }
        if (!artyAutoHitHexes.contains(coords)
                && artyAutoHitHexes.size() < 5
                && clientgui
                        .doYesNoDialog(
                                Messages
                                        .getString("SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.title"), //$NON-NLS-1$
                                Messages
                                        .getString(
                                                "SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.message", new Object[] { coords.getBoardNum() }))) { //$NON-NLS-1$
            artyAutoHitHexes.addElement(coords);
            setArtyEnabled(5 - artyAutoHitHexes.size());
            client.game.getBoard().addSpecialHexDisplay(
                    coords,
                    new SpecialHexDisplay(
                            SpecialHexDisplay.Type.ARTILLERY_AUTOHIT,
                            SpecialHexDisplay.NO_ROUND, client.getLocalPlayer()
                                    .getName(),
                            "Artilery autohit, better text later"));
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
        if (!client.isMyTurn()
                || (b.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
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

        if (client.isMyTurn()) {
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

        if (client.isMyTurn()
                && client.game.getPhase() != IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
            endMyTurn();
        }
        if (client.game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
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

        if (statusBarActionPerformed(ev, client)) {
            return;
        }

        if (!client.isMyTurn()) {
            // odd...
            return;
        }

        if (ev.getSource().equals(butDone)) {
            endMyTurn();
            client.sendArtyAutoHitHexes(artyAutoHitHexes);
            client.sendPlayerInfo();
        }
    } // End public void actionPerformed(ActionEvent ev)

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        // ignore
    }

    public void keyReleased(KeyEvent ev) {
        // ignore
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
    }

    private void setArtyEnabled(int nbr) {
        butA
                .setText(Messages
                        .getString(
                                "SelectArtyAutoHitHexDisplay.designatedTargets", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butA.setEnabled(nbr > 0);
        // clientgui.getMenuBar().setSelectArtyAutoHitHexEnabled(nbr);
    }

    /**
     * Retrieve the "Done" button of this object.
     * 
     * @return the <code>javax.swing.JButton</code> that activates this object's
     *         "Done" action.
     */
    public JButton getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

}
