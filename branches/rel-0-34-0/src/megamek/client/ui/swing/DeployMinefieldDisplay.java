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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.Terrains;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class DeployMinefieldDisplay extends StatusBarPhaseDisplay implements
        DoneButtoned, KeyListener {
    private static final long serialVersionUID = -1243277953037374936L;

    // Action command names
    public static final String DEPLOY_MINE_CONV = "deployMineConv"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_COM = "deployMineCom"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_VIBRA = "deployMineVibra"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_ACTIVE = "deployMineActive"; //$NON-NLS-1$
    public static final String DEPLOY_MINE_INFERNO = "deployMineInferno"; //$NON-NLS-1$
    public static final String REMOVE_MINES = "removeMines"; //$NON-NLS-1$

    // parent game
    public ClientGUI clientgui;
    private Client client;

    // buttons
    private JPanel panButtons;

    private JButton butC;
    private JButton butM;
    // private JButton butSpace;
    // private JButton butSpace1;
    // private JButton butSpace2;
    // private JButton butSpace3;
    private JButton butV;
    private JButton butA;
    private JButton butI;
    private JButton butRemove;
    private JButton butUnload;
    private JButton butDone;

    // is the shift key held?
    private boolean deployM = false;
    private boolean deployC = false;
    private boolean deployV = false;
    private boolean deployA = false;
    private boolean deployI = false;
    private boolean remove = false;

    private Player p;
    private Vector<Minefield> deployedMinefields = new Vector<Minefield>();

    /**
     * Creates and lays out a new deployment phase display for the specified
     * client.
     */
    public DeployMinefieldDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        client = clientgui.getClient();
        client.game.addGameListener(this);

        // Listener is added in the ClientGUI#switchPanel
        // clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages
                .getString("DeployMinefieldDisplay.waitingForDeployMinefieldPhase")); //$NON-NLS-1$

        p = client.getLocalPlayer();

        butM = new JButton(
                Messages
                        .getString(
                                "DeploymentDisplay.buttonMinefield", new Object[] { new Integer(p.getNbrMFConventional()) })); //$NON-NLS-1$
        butM.addActionListener(this);
        butM.setActionCommand(DEPLOY_MINE_CONV);
        butM.setEnabled(false);

        // butSpace = new JButton(".");
        // butSpace.setEnabled(false);
        // butSpace1 = new JButton(".");
        // butSpace1.setEnabled(false);
        // butSpace2 = new JButton(".");
        // butSpace2.setEnabled(false);
        // butSpace3 = new JButton(".");
        // butSpace3.setEnabled(false);

        butC = new JButton(
                Messages
                        .getString(
                                "DeploymentDisplay.buttonCommand", new Object[] { new Integer(p.getNbrMFCommand()) })); //$NON-NLS-1$
        butC.addActionListener(this);
        butC.setActionCommand(DEPLOY_MINE_COM);
        butC.setEnabled(false);

        butUnload = new JButton("."); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);

        butV = new JButton(
                Messages
                        .getString(
                                "DeploymentDisplay.buttonVibrabomb", new Object[] { new Integer(p.getNbrMFVibra()) })); //$NON-NLS-1$
        butV.addActionListener(this);
        butV.setActionCommand(DEPLOY_MINE_VIBRA);
        butV.setEnabled(false);

        butA = new JButton(
                Messages
                        .getString(
                                "DeploymentDisplay.buttonActive", new Object[] { new Integer(p.getNbrMFActive()) })); //$NON-NLS-1$
        butA.addActionListener(this);
        butA.setActionCommand(DEPLOY_MINE_ACTIVE);
        butA.setEnabled(false);

        butI = new JButton(
                Messages
                        .getString(
                                "DeploymentDisplay.buttonInferno", new Object[] { new Integer(p.getNbrMFInferno()) })); //$NON-NLS-1$
        butI.addActionListener(this);
        butI.setActionCommand(DEPLOY_MINE_INFERNO);
        butI.setEnabled(false);

        butRemove = new JButton(Messages
                .getString("DeploymentDisplay.buttonRemove")); //$NON-NLS-1$
        butRemove.addActionListener(this);
        butRemove.setActionCommand(REMOVE_MINES);
        butRemove.setEnabled(false);

        butDone = new JButton(Messages.getString("DeployMinefieldDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(0, 8));
        panButtons.add(butM);
        panButtons.add(butC);
        panButtons.add(butV);
        panButtons.add(butI);
        panButtons.add(butA);
        panButtons.add(butRemove);
        // panButtons.add(butSpace);
        // panButtons.add(butSpace1);
        // panButtons.add(butSpace2);
        // panButtons.add(butSpace3);
        // panButtons.add(butDone);

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
        p = client.getLocalPlayer();// necessary to make it work after resets.
        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());
        setActiveEnabled(p.getNbrMFActive());
        setInfernoEnabled(p.getNbrMFInferno());
        setRemoveMineEnabled(true);
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
        setActiveEnabled(0);
        setInfernoEnabled(0);
        setRemoveMineEnabled(false);

        butDone.setEnabled(false);
        butUnload.setEnabled(false);
    }

    private void deployMinefield(Coords coords) {
        if (!client.game.getBoard().contains(coords)) {
            return;
        }

        // check if this is a water hex
        boolean sea = false;
        IHex hex = client.game.getBoard().getHex(coords);
        if (hex.containsTerrain(Terrains.WATER)) {
            sea = true;
        }

        if (remove) {
            if (!client.game.containsMinefield(coords)) {
                return;
            }
            Enumeration<?> mfs = client.game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<Minefield>();
            while (mfs.hasMoreElements()) {
                Minefield mf = (Minefield) mfs.nextElement();
                if (mf.getPlayerId() == client.getLocalPlayer().getId()) {
                    butDone.setEnabled(false);
                    mfRemoved.add(mf);
                    deployedMinefields.removeElement(mf);
                    if (mf.getType() == Minefield.TYPE_CONVENTIONAL) {
                        p.setNbrMFConventional(p.getNbrMFConventional() + 1);
                    } else if (mf.getType() == Minefield.TYPE_COMMAND_DETONATED) {
                        p.setNbrMFCommand(p.getNbrMFCommand() + 1);
                    } else if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                        p.setNbrMFVibra(p.getNbrMFVibra() + 1);
                    } else if (mf.getType() == Minefield.TYPE_ACTIVE) {
                        p.setNbrMFActive(p.getNbrMFActive() + 1);
                    } else if (mf.getType() == Minefield.TYPE_INFERNO) {
                        p.setNbrMFInferno(p.getNbrMFInferno() + 1);
                    }
                }
            }

            for (Minefield mf : mfRemoved) {
                client.game.removeMinefield(mf);
            }
        } else {
            // first check that there is not already a mine of this type
            // deployed
            Enumeration<?> mfs = client.game.getMinefields(coords).elements();
            while (mfs.hasMoreElements()) {
                Minefield mf = (Minefield) mfs.nextElement();
                if ((deployM && mf.getType() == Minefield.TYPE_CONVENTIONAL)
                        || (deployC && mf.getType() == Minefield.TYPE_COMMAND_DETONATED)
                        || (deployV && mf.getType() == Minefield.TYPE_VIBRABOMB)
                        || (deployA && mf.getType() == Minefield.TYPE_ACTIVE)
                        || (deployI && mf.getType() == Minefield.TYPE_INFERNO)) {
                    clientgui
                            .doAlertDialog(
                                    Messages
                                            .getString("DeployMinefieldDisplay.IllegalPlacement"), Messages.getString("DeployMinefieldDisplay.DuplicateMinefield")); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
            }

            Minefield mf;
            if (sea && !(deployM || deployI)) {
                clientgui
                        .doAlertDialog(
                                Messages
                                        .getString("DeployMinefieldDisplay.IllegalPlacement"), Messages.getString("DeployMinefieldDisplay.WaterPlacement")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            int depth = 0;
            if (deployM) {
                if (sea) {
                    SeaMineDepthDialog smd = new SeaMineDepthDialog(
                            clientgui.frame, hex.depth());
                    smd.setVisible(true);

                    depth = smd.getDepth();
                }
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                mf = Minefield.createMinefield(coords, p.getId(),
                        Minefield.TYPE_CONVENTIONAL, mfd.getDensity(), sea,
                        depth);
                p.setNbrMFConventional(p.getNbrMFConventional() - 1);
            } else if (deployC) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                mf = Minefield.createMinefield(coords, p.getId(),
                        Minefield.TYPE_COMMAND_DETONATED, mfd.getDensity(),
                        sea, depth);
                p.setNbrMFCommand(p.getNbrMFCommand() - 1);
            } else if (deployA) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                mf = Minefield.createMinefield(coords, p.getId(),
                        Minefield.TYPE_ACTIVE, mfd.getDensity());
                p.setNbrMFActive(p.getNbrMFActive() - 1);
            } else if (deployI) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                mf = Minefield.createMinefield(coords, p.getId(),
                        Minefield.TYPE_INFERNO, mfd.getDensity(), sea, depth);
                p.setNbrMFInferno(p.getNbrMFInferno() - 1);
            } else if (deployV) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);

                mf = Minefield.createMinefield(coords, p.getId(),
                        Minefield.TYPE_VIBRABOMB, mfd.getDensity(), vsd
                                .getSetting());
                p.setNbrMFVibra(p.getNbrMFVibra() - 1);
            } else {
                return;
            }
            client.game.addMinefield(mf);
            deployedMinefields.addElement(mf);
            clientgui.bv.refreshDisplayables();
        }

        if (p.getNbrMFConventional() == 0 && p.getNbrMFCommand() == 0
                && p.getNbrMFVibra() == 0 && p.getNbrMFActive() == 0
                && p.getNbrMFInferno() == 0) {
            butDone.setEnabled(true);
        }

        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());
        setActiveEnabled(p.getNbrMFActive());
        setInfernoEnabled(p.getNbrMFInferno());

        if (p.getNbrMFConventional() == 0) {
            deployM = false;
        }
        if (p.getNbrMFCommand() == 0) {
            deployC = false;
        }
        if (p.getNbrMFVibra() == 0) {
            deployV = false;
        }
        if (p.getNbrMFActive() == 0) {
            deployA = false;
        }
        if (p.getNbrMFInferno() == 0) {
            deployI = false;
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
        deployMinefield(b.getCoords());
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
                    .getString("DeployMinefieldDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            setStatusBarText(Messages
                    .getString(
                            "DeployMinefieldDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn()
                && client.game.getPhase() != IGame.Phase.PHASE_DEPLOY_MINEFIELDS) {
            endMyTurn();
        }
        if (client.game.getPhase() == IGame.Phase.PHASE_DEPLOY_MINEFIELDS) {
            setStatusBarText(Messages
                    .getString("DeployMinefieldDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
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
            client.sendDeployMinefields(deployedMinefields);
            client.sendPlayerInfo();
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_CONV)) {
            deployM = true;
            deployC = false;
            deployV = false;
            deployA = false;
            deployI = false;
            remove = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_COM)) {
            deployM = false;
            deployC = true;
            deployV = false;
            deployA = false;
            deployI = false;
            remove = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_VIBRA)) {
            deployM = false;
            deployC = false;
            deployV = true;
            deployA = false;
            deployI = false;
            remove = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_ACTIVE)) {
            deployM = false;
            deployC = false;
            deployV = false;
            deployA = true;
            deployI = false;
            remove = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_INFERNO)) {
            deployM = false;
            deployC = false;
            deployV = false;
            deployA = false;
            deployI = true;
            remove = false;
        }
        if (ev.getActionCommand().equals(REMOVE_MINES)) {
            deployM = false;
            deployC = false;
            deployV = false;
            deployA = false;
            deployI = false;
            remove = true;
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

    private void setConventionalEnabled(int nbr) {
        butM
                .setText(Messages
                        .getString(
                                "DeploymentDisplay.buttonMinefield", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butM.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployConventionalEnabled(nbr);
    }

    private void setCommandEnabled(int nbr) {
        butC
                .setText(Messages
                        .getString(
                                "DeploymentDisplay.buttonCommand", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butC.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployCommandEnabled(nbr);
    }

    private void setVibrabombEnabled(int nbr) {
        butV
                .setText(Messages
                        .getString(
                                "DeploymentDisplay.buttonVibrabomb", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butV.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployVibrabombEnabled(nbr);
    }

    private void setActiveEnabled(int nbr) {
        butA
                .setText(Messages
                        .getString(
                                "DeploymentDisplay.buttonActive", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butA.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployActiveEnabled(nbr);
    }

    private void setInfernoEnabled(int nbr) {
        butI
                .setText(Messages
                        .getString(
                                "DeploymentDisplay.buttonInferno", new Object[] { new Integer(nbr) })); //$NON-NLS-1$
        butI.setEnabled(nbr > 0);
        clientgui.getMenuBar().setDeployInfernoEnabled(nbr);
    }

    private void setRemoveMineEnabled(boolean enable) {
        butRemove.setEnabled(enable);
        // clientgui.getMenuBar().setRemoveMineEnabled(enable);
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
