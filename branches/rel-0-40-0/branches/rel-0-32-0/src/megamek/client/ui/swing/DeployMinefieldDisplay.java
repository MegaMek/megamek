/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Vector;

public class DeployMinefieldDisplay
        extends StatusBarPhaseDisplay
        implements BoardViewListener, ActionListener, DoneButtoned,
        KeyListener, GameListener, Distractable {
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // Action command names
    public static final String DEPLOY_MINE_CONV = "deployMineConv"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_COM = "deployMineCom"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_VIBRA = "deployMineVibra"; //$NON-NLS-1$

    // parent game
    public ClientGUI clientgui;
    private Client client;

    // buttons
    private JPanel panButtons;

    private JButton butC;
    private JButton butM;
//     private JButton            butSpace;
//     private JButton            butSpace1;
//     private JButton            butSpace2;
//     private JButton            butSpace3;
    private JButton butV;
    private JButton butUnload;
    private JButton butDone;

    // is the shift key held?
    private boolean deployM = false;
    private boolean deployC = false;
    private boolean deployV = false;

    private Player p;
    private Vector deployedMinefields = new Vector();

    /**
     * Creates and lays out a new deployment phase display
     * for the specified client.
     */
    public DeployMinefieldDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        client = clientgui.getClient();
        client.game.addGameListener(this);

        //Listener is added in the ClientGUI#switchPanel
        //clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages.getString("DeployMinefieldDisplay.waitingForDeployMinefieldPhase")); //$NON-NLS-1$

        p = client.getLocalPlayer();

        butM = new JButton(Messages.getString("DeploymentDisplay.buttonMinefield", new Object[]{new Integer(p.getNbrMFConventional())})); //$NON-NLS-1$
        butM.addActionListener(this);
        butM.setActionCommand(DEPLOY_MINE_CONV);
        butM.setEnabled(false);

//         butSpace = new JButton(".");
//         butSpace.setEnabled(false);
//         butSpace1 = new JButton(".");
//         butSpace1.setEnabled(false);
//         butSpace2 = new JButton(".");
//         butSpace2.setEnabled(false);
//         butSpace3 = new JButton(".");
//         butSpace3.setEnabled(false);

        butC = new JButton(Messages.getString("DeploymentDisplay.buttonCommand", new Object[]{new Integer(p.getNbrMFCommand())})); //$NON-NLS-1$
        butC.addActionListener(this);
        butC.setActionCommand(DEPLOY_MINE_COM);
        butC.setEnabled(false);

        butUnload = new JButton("."); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);

        butV = new JButton(Messages.getString("DeploymentDisplay.buttonVibrabomb", new Object[]{new Integer(p.getNbrMFVibra())})); //$NON-NLS-1$
        butV.addActionListener(this);
        butV.setActionCommand(DEPLOY_MINE_VIBRA);
        butV.setEnabled(false);

        butDone = new JButton(Messages.getString("DeployMinefieldDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(0, 8));
        panButtons.add(butM);
        panButtons.add(butC);
        panButtons.add(butV);
//         panButtons.add(butSpace);
//         panButtons.add(butSpace1);
//         panButtons.add(butSpace2);
//         panButtons.add(butSpace3);
//         panButtons.add(butDone);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(clientgui.bv, gridbag, c);

//         c.weightx = 1.0;    c.weighty = 0;
//         c.gridwidth = 1;
//         addBag(client.cb.getComponent(), gridbag, c);

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

    private void addBag(JComponent comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        p = client.getLocalPlayer();//necessary to make it work after resets.
        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());
        if (!p.hasMinefields()) {
            butDone.setEnabled(true);
        }
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
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
        setConventionalEnabled(0);
        setCommandEnabled(0);
        setVibrabombEnabled(0);

        butDone.setEnabled(false);
        butUnload.setEnabled(false);
    }

    private void deployMinefield(Coords coords) {
        if (!client.game.getBoard().contains(coords)) {
            return;
        }

        if (client.game.containsMinefield(coords)) {
            Minefield mf = (Minefield) client.game.getMinefields(coords).elementAt(0);
            if (mf.getPlayerId() == client.getLocalPlayer().getId()) {
                butDone.setEnabled(false);
                client.game.removeMinefield(mf);
                deployedMinefields.removeElement(mf);
                switch (mf.getType()) {
                    case (Minefield.TYPE_CONVENTIONAL):
                        deployM = true;
                        deployC = false;
                        deployV = false;
                        p.setNbrMFConventional(p.getNbrMFConventional() + 1);
                        break;
                    case (Minefield.TYPE_COMMAND_DETONATED):
                        deployM = false;
                        deployC = true;
                        deployV = false;
                        p.setNbrMFCommand(p.getNbrMFCommand() + 1);
                        break;
                    case (Minefield.TYPE_VIBRABOMB):
                        deployM = false;
                        deployC = false;
                        deployV = true;
                        p.setNbrMFVibra(p.getNbrMFVibra() + 1);
                        break;
                }
            }
        } else {
            Minefield mf;

            if (deployM) {
                mf = Minefield.createConventionalMF(coords, p.getId());
                p.setNbrMFConventional(p.getNbrMFConventional() - 1);
            } else if (deployC) {
                mf = Minefield.createCommandDetonatedMF(coords, p.getId());
                p.setNbrMFCommand(p.getNbrMFCommand() - 1);
            } else if (deployV) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.frame);
                vsd.setVisible(true);

                // Hack warning...              
                clientgui.bv.stopScrolling();
                mf = Minefield.createVibrabombMF(coords, p.getId(), vsd.getSetting());
                p.setNbrMFVibra(p.getNbrMFVibra() - 1);
            } else {
                return;
            }
            client.game.addMinefield(mf);
            deployedMinefields.addElement(mf);
            clientgui.bv.repaint();
        }

        if (p.getNbrMFConventional() == 0 &&
                p.getNbrMFCommand() == 0 &&
                p.getNbrMFVibra() == 0) {
            butDone.setEnabled(true);
        }

        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());

        if (p.getNbrMFConventional() == 0) {
            deployM = false;
        }
        if (p.getNbrMFCommand() == 0) {
            deployC = false;
        }
        if (p.getNbrMFVibra() == 0) {
            deployV = false;
        }

    }

    //
    // BoardListener
    //
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (b.getType() != BoardViewEvent.BOARD_HEX_DRAGGED) {
            return;
        }
        
        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
        
        // check for a deployment
        clientgui.getBoardView().select(b.getCoords());
        deployMinefield(b.getCoords());
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
        }
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() &&
                client.game.getPhase() != IGame.PHASE_DEPLOY_MINEFIELDS) {
            endMyTurn();
        }
        if (client.game.getPhase() == IGame.PHASE_DEPLOY_MINEFIELDS) {
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
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

        if (statusBarActionPerformed(ev, client))
            return;

        if (!client.isMyTurn()) {
            // odd...
            return;
        }

        if (ev.getSource().equals(butDone)) {
            endMyTurn();
            client.sendDeployMinefields(deployedMinefields);
            client.sendPlayerInfo();
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_CONV)) {
            deployM = true;
            deployC = false;
            deployV = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_COM)) {
            deployM = false;
            deployC = true;
            deployV = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_VIBRA)) {
            deployM = false;
            deployC = false;
            deployV = true;
        }
    } // End public void actionPerformed(ActionEvent ev)

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

    private void setConventionalEnabled(int nbr) {
        butM.setText(Messages.getString("DeploymentDisplay.buttonMinefield", new Object[]{new Integer(nbr)})); //$NON-NLS-1$
        butM.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployConventionalEnabled(nbr);
    }

    private void setCommandEnabled(int nbr) {
        butC.setText(Messages.getString("DeploymentDisplay.buttonCommand", new Object[]{new Integer(nbr)})); //$NON-NLS-1$
        butC.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployCommandEnabled(nbr);
    }

    private void setVibrabombEnabled(int nbr) {
        butV.setText(Messages.getString("DeploymentDisplay.buttonVibrabomb", new Object[]{new Integer(nbr)})); //$NON-NLS-1$
        butV.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployVibrabombEnabled(nbr);
    }

    /**
     * Determine if the listener is currently distracted.
     *
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param distracted <code>true</code> if the listener should ignore events
     *                   <code>false</code> if the listener should pay attention again.
     *                   Events that occured while the listener was distracted NOT
     *                   going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return the <code>javax.swing.JButton</code> that activates this
     *         object's "Done" action.
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
