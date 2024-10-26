/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.ICarryable;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.Terrains;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class DeployMinefieldDisplay extends StatusBarPhaseDisplay {
    private static final long serialVersionUID = -1243277953037374936L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the deploy minefield phase.  Each command has a string
     * for the command.
     * @author arlith
     */
    public static enum DeployMinefieldCommand implements PhaseCommand {
    	COMMAND_NONE("noCommand"),
        DEPLOY_MINE_CONV("deployMineConv"),
        DEPLOY_MINE_COM("deployMineCom"),
        DEPLOY_MINE_VIBRA("deployMineVibra"),
        DEPLOY_MINE_ACTIVE("deployMineActive"),
        DEPLOY_MINE_INFERNO("deployMineInferno"),
        DEPLOY_CARRYABLE("deployCarriable"),
        REMOVE_MINES("removeMines");

    	private static DeployMinefieldCommand[] actualCommands =
    		{ DEPLOY_MINE_CONV, DEPLOY_MINE_COM, DEPLOY_MINE_VIBRA, DEPLOY_MINE_ACTIVE,
    				DEPLOY_MINE_INFERNO, DEPLOY_CARRYABLE, REMOVE_MINES };

        /**
         * Priority that determines this buttons order
         */
        public int priority;
        String cmd;

        DeployMinefieldCommand(String c) {
            cmd = c;
        }

        /**
         * Given a string, figure out the command value
         */
        public static DeployMinefieldCommand fromString(String command) {
        	for (DeployMinefieldCommand value : values()) {
        		if (value.getCmd().equals(command)) {
        			return value;
        		}
        	}

        	return null;
        }

        /**
         * Get all the commands that aren't NO-OP
         */
        public static DeployMinefieldCommand[] getActualCommands() {
        	return actualCommands;
        }

        @Override
        public String getCmd() {
            return cmd;
        }
        @Override
        public int getPriority() {
            return priority;
        }
        @Override
        public void setPriority(int p) {
            priority = p;
        }

        @Override
        public String toString() {
            return Messages.getString("DeployMinefieldDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            return "";
        }
    }

    // buttons
    protected Map<DeployMinefieldCommand,MegaMekButton> buttons;

    DeployMinefieldCommand currentCommand = DeployMinefieldCommand.COMMAND_NONE;

    private boolean deployingConventionalMinefields() {
    	return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_CONV);
    }

    private boolean deployingActiveMinefields() {
    	return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_ACTIVE);
    }

    private boolean deployingInfernoMinefields() {
    	return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_INFERNO);
    }

    private boolean deployingCommandMinefields() {
    	return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_COM);
    }

    private boolean deployingVibrabombMinefields() {
    	return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_VIBRA);
    }

    private Player p;
    private Vector<Minefield> deployedMinefields = new Vector<>();

    protected final ClientGUI clientgui;

    /**
     * Creates and lays out a new deployment phase display for the specified
     * clientgui.getClient().
     */
    public DeployMinefieldDisplay(ClientGUI clientgui) {
        super(clientgui);
        this.clientgui = clientgui;
        clientgui.getClient().getGame().addGameListener(this);

        setupStatusBar(Messages
                .getString("DeployMinefieldDisplay.waitingForDeployMinefieldPhase"));

        p = clientgui.getClient().getLocalPlayer();

        setButtons();
        setButtonsTooltips();

        butDone.setText(Messages.getString("DeployMinefieldDisplay.Done"));
        butDone.setEnabled(false);

        setupButtonPanel();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (DeployMinefieldCommand.getActualCommands().length * 1.25 + 0.5));
        for (DeployMinefieldCommand cmd : DeployMinefieldCommand.getActualCommands()) {
        	buttons.put(cmd, createButton(cmd.getCmd(), "DeployMinefieldDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size()+0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (DeployMinefieldCommand cmd : DeployMinefieldCommand.getActualCommands()) {
            String tt = createToolTip(cmd.getCmd(), "DeployMinefieldDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        for (DeployMinefieldCommand cmd : DeployMinefieldCommand.getActualCommands()) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        p = clientgui.getClient().getLocalPlayer();// necessary to make it work after resets.
        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());
        setActiveEnabled(p.getNbrMFActive());
        setInfernoEnabled(p.getNbrMFInferno());
        setCarryableEnabled(p.getGroundObjectsToPlace().size());
        setRemoveMineEnabled(true);
        butDone.setEnabled(true);

        startTimer();
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        stopTimer();

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
        setCarryableEnabled(0);
        setRemoveMineEnabled(false);

        butDone.setEnabled(false);
    }

    private void deployMinefield(Coords coords) {
    	Game game = clientgui.getClient().getGame();

        if (!game.getBoard().contains(coords)) {
            return;
        }

        // check if this is a water hex
        boolean sea = false;
        Hex hex = game.getBoard().getHex(coords);
        if (hex.containsTerrain(Terrains.WATER)) {
            sea = true;
        }

        if (currentCommand == DeployMinefieldCommand.REMOVE_MINES) {
            if (!game.containsMinefield(coords) &&
            		game.getGroundObjects(coords).size() == 0) {
                return;
            }
            Enumeration<?> mfs = game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (mfs.hasMoreElements()) {
                Minefield mf = (Minefield) mfs.nextElement();
                if (mf.getPlayerId() == clientgui.getClient().getLocalPlayer().getId()) {
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
            	game.removeMinefield(mf);
            }

            // remove all carryables here as well and put them back to the player
            for (ICarryable carryable : game.getGroundObjects(coords)) {
            	p.getGroundObjectsToPlace().add(carryable);
            }

            game.getGroundObjects().remove(coords);

            clientgui.showGroundObjects(game.getGroundObjects());

        } else if (currentCommand == DeployMinefieldCommand.DEPLOY_CARRYABLE) {
        	List<ICarryable> groundObjects = p.getGroundObjectsToPlace();

        	ICarryable toDeploy = groundObjects.get(0);

        	if (groundObjects.size() > 1) {
        		String title = "Choose Cargo to Place";
                String body = "Choose the cargo to place:";
                toDeploy = (ICarryable) JOptionPane.showInputDialog(clientgui.getFrame(),
                        body, title, JOptionPane.QUESTION_MESSAGE, null,
                        groundObjects.toArray(), groundObjects.get(0));
        	}

        	game.placeGroundObject(coords, toDeploy);
        	groundObjects.remove(toDeploy);

        	if (groundObjects.size() <= 0) {
        		currentCommand = DeployMinefieldCommand.COMMAND_NONE;
        	}

        	clientgui.showGroundObjects(game.getGroundObjects());
        } else {
        	// first check that there is not already a mine of this type
            // deployed
            Enumeration<?> mfs = game.getMinefields(coords).elements();
            while (mfs.hasMoreElements()) {
                Minefield mf = (Minefield) mfs.nextElement();
                if ((deployingConventionalMinefields() && (mf.getType() == Minefield.TYPE_CONVENTIONAL))
                        || (deployingCommandMinefields() && (mf.getType() == Minefield.TYPE_COMMAND_DETONATED))
                        || (deployingVibrabombMinefields() && (mf.getType() == Minefield.TYPE_VIBRABOMB))
                        || (deployingActiveMinefields() && (mf.getType() == Minefield.TYPE_ACTIVE))
                        || (deployingInfernoMinefields() && (mf.getType() == Minefield.TYPE_INFERNO))) {
                    clientgui.doAlertDialog(Messages.getString("DeployMinefieldDisplay.IllegalPlacement"),
                            Messages.getString("DeployMinefieldDisplay.DuplicateMinefield"));
                    return;
                }
            }

            Minefield mf = null;
            if (sea && !(deployingConventionalMinefields() || deployingInfernoMinefields())) {
                clientgui.doAlertDialog(Messages.getString("DeployMinefieldDisplay.IllegalPlacement"),
                        Messages.getString("DeployMinefieldDisplay.WaterPlacement"));
                return;
            }
            int depth = 0;
            if (deployingConventionalMinefields()) {
                if (sea) {
                    SeaMineDepthDialog smd = new SeaMineDepthDialog(
                            clientgui.frame, hex.depth());
                    smd.setVisible(true);

                    depth = smd.getDepth();
                }
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                if (mfd.getDensity() > 0) {
                    mf = Minefield.createMinefield(coords, p.getId(),
                            Minefield.TYPE_CONVENTIONAL, mfd.getDensity(), sea,
                            depth);
                    p.setNbrMFConventional(p.getNbrMFConventional() - 1);

                    if (p.getNbrMFConventional() <= 0) {
                    	currentCommand = DeployMinefieldCommand.COMMAND_NONE;
                    }
                }
            } else if (deployingCommandMinefields()) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                if (mfd.getDensity() > 0) {
                    mf = Minefield.createMinefield(coords, p.getId(),
                            Minefield.TYPE_COMMAND_DETONATED, mfd.getDensity(),
                            sea, depth);
                    p.setNbrMFCommand(p.getNbrMFCommand() - 1);

                    if (p.getNbrMFCommand() <= 0) {
                    	currentCommand = DeployMinefieldCommand.COMMAND_NONE;
                    }
                }
            } else if (deployingActiveMinefields()) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                if (mfd.getDensity() > 0) {
                    mf = Minefield.createMinefield(coords, p.getId(),
                            Minefield.TYPE_ACTIVE, mfd.getDensity());
                    p.setNbrMFActive(p.getNbrMFActive() - 1);

                    if (p.getNbrMFActive() <= 0) {
                    	currentCommand = DeployMinefieldCommand.COMMAND_NONE;
                    }
                }
            } else if (deployingInfernoMinefields()) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                if (mfd.getDensity() > 0) {
                    mf = Minefield.createMinefield(coords, p.getId(),
                            Minefield.TYPE_INFERNO, mfd.getDensity(), sea,
                            depth);
                    p.setNbrMFInferno(p.getNbrMFInferno() - 1);

                    if (p.getNbrMFInferno() <= 0) {
                    	currentCommand = DeployMinefieldCommand.COMMAND_NONE;
                    }
                }
            } else if (deployingVibrabombMinefields()) {
                MineDensityDialog mfd = new MineDensityDialog(clientgui.frame);
                mfd.setVisible(true);

                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);

                if (mfd.getDensity() > 0) {
                    mf = Minefield.createMinefield(coords, p.getId(),
                            Minefield.TYPE_VIBRABOMB, mfd.getDensity(),
                            vsd.getSetting());
                    p.setNbrMFVibra(p.getNbrMFVibra() - 1);

                    if (p.getNbrMFVibra() <= 0) {
                    	currentCommand = DeployMinefieldCommand.COMMAND_NONE;
                    }
                }
            } else {
                return;
            }
            if (mf != null) {
                mf.setWeaponDelivered(false);
                game.addMinefield(mf);
                deployedMinefields.addElement(mf);
            }
            clientgui.getBoardView().refreshDisplayables();
        }

        setConventionalEnabled(p.getNbrMFConventional());
        setCommandEnabled(p.getNbrMFCommand());
        setVibrabombEnabled(p.getNbrMFVibra());
        setActiveEnabled(p.getNbrMFActive());
        setInfernoEnabled(p.getNbrMFInferno());
        setCarryableEnabled(p.getGroundObjectsToPlace().size());
    }

    @Override
    public void clear() {

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
                || ((b.getButton() != MouseEvent.BUTTON1))) {
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

        if (clientgui.getClient().isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_your_turn"));
            clientgui.bingMyTurn();
        } else {
            String playerName;
            if (e.getPlayer() != null) {
                playerName = e.getPlayer().getName();
            } else {
                playerName = "Unknown";
            }
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_others_turn", playerName));
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && !clientgui.getClient().getGame().getPhase().isDeployMinefields()) {
            endMyTurn();
        }

        if (clientgui.getClient().getGame().getPhase().isDeployMinefields()) {
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.waitingForDeploymentPhase"));
        }
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            // odd...
            return;
        }

        currentCommand = DeployMinefieldCommand.fromString(ev.getActionCommand());
    } // End public void actionPerformed(ActionEvent ev)

    @Override
    public void ready() {
        endMyTurn();
        clientgui.getClient().sendDeployGroundObjects(clientgui.getClient().getGame().getGroundObjects());
        clientgui.getClient().sendDeployMinefields(deployedMinefields);
        clientgui.getClient().sendPlayerInfo();
    }

    private void setConventionalEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_CONV).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_CONV.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_CONV).setEnabled(nbr > 0);
    }

    private void setCommandEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_COM).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_COM.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_COM).setEnabled(nbr > 0);
    }

    private void setVibrabombEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_VIBRA).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_VIBRA.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_VIBRA).setEnabled(nbr > 0);
    }

    private void setActiveEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_ACTIVE).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_ACTIVE.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_ACTIVE).setEnabled(nbr > 0);
    }

    private void setInfernoEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_INFERNO).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_INFERNO.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_INFERNO).setEnabled(nbr > 0);
    }

    private void setCarryableEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_CARRYABLE).setText(Messages.getString(
                "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_CARRYABLE.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_CARRYABLE).setEnabled(nbr > 0);
    }

    private void setRemoveMineEnabled(boolean enable) {
        buttons.get(DeployMinefieldCommand.REMOVE_MINES).setEnabled(enable);
    }

    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }
}
