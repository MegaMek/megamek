/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.phaseDisplay;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.dialogs.phaseDisplay.EMPMineSettingDialog;
import megamek.client.ui.dialogs.phaseDisplay.MineDensityDialog;
import megamek.client.ui.dialogs.phaseDisplay.SeaMineDepthDialog;
import megamek.client.ui.dialogs.phaseDisplay.VibrabombSettingDialog;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Terrains;

public class DeployMinefieldDisplay extends StatusBarPhaseDisplay {
    @Serial
    private static final long serialVersionUID = -1243277953037374936L;

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the deployment minefield
     * phase.  Each command has a string for the command.
     *
     * @author arlith
     */
    public enum DeployMinefieldCommand implements PhaseCommand {
        COMMAND_NONE("noCommand"),
        DEPLOY_MINE_CONV("deployMineConv"),
        DEPLOY_MINE_COM("deployMineCom"),
        DEPLOY_MINE_VIBRA("deployMineVibra"),
        DEPLOY_MINE_ACTIVE("deployMineActive"),
        DEPLOY_MINE_INFERNO("deployMineInferno"),
        DEPLOY_MINE_EMP("deployMineEMP"),
        DEPLOY_CARRYABLE("deployCarriable"),
        REMOVE_MINES("removeMines");

        private static final DeployMinefieldCommand[] actualCommands =
              { DEPLOY_MINE_CONV, DEPLOY_MINE_COM, DEPLOY_MINE_VIBRA, DEPLOY_MINE_ACTIVE,
                DEPLOY_MINE_INFERNO, DEPLOY_MINE_EMP, DEPLOY_CARRYABLE, REMOVE_MINES };

        /**
         * Priority that determines this buttons order
         */
        public int priority;
        final String cmd;

        DeployMinefieldCommand(String c) {
            cmd = c;
        }

        /**
         * Given a string, figure out the command value
         * @param command                   String name of the requested command
         * @return DeployMinefieldCommand   found command or COMMAND_NONE
         */
        public static DeployMinefieldCommand fromString(String command) {
            for (DeployMinefieldCommand value : values()) {
                if (value.getCmd().equals(command)) {
                    return value;
                }
            }

            return COMMAND_NONE;
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
    protected Map<DeployMinefieldCommand, MegaMekButton> buttons;

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

    private boolean deployingEMPMinefields() {
        return currentCommand.equals(DeployMinefieldCommand.DEPLOY_MINE_EMP);
    }

    private Player p;
    private final Vector<Minefield> deployedMinefields = new Vector<>();

    protected final ClientGUI clientgui;

    /**
     * Creates and lays out a new deployment phase display for the specified clientGUI.getClient().
     */
    public DeployMinefieldDisplay(ClientGUI clientgui) {
        super(clientgui);
        this.clientgui = clientgui;
        clientgui.getClient().getGame().addGameListener(this);
        setupStatusBar(Messages.getString("DeployMinefieldDisplay.waitingForDeployMinefieldPhase"));
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
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
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
        setEMPEnabled(p.getNbrMFEMP());
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
        disableButtons();
        clientgui.onAllBoardViews(IBoardView::clearMarkedHexes);
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
        setEMPEnabled(0);
        setCarryableEnabled(0);
        setRemoveMineEnabled(false);

        butDone.setEnabled(false);
    }

    private void deployMinefield(BoardViewEvent event) {
        Game game = clientgui.getClient().getGame();
        if (!game.hasBoardLocation(event.getBoardLocation())) {
            return;
        }

        // check if this is a water hex
        boolean sea = false;
        Hex hex = game.getHex(event.getBoardLocation());
        if (hex.containsTerrain(Terrains.WATER)) {
            sea = true;
        }

        Coords coords = event.getCoords();

        if (currentCommand == DeployMinefieldCommand.REMOVE_MINES) {
            if (!game.containsMinefield(coords) &&
                  game.getGroundObjects(coords).isEmpty()) {
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
                    } else if (mf.getType() == Minefield.TYPE_EMP) {
                        p.setNbrMFEMP(p.getNbrMFEMP() + 1);
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

            if (groundObjects.isEmpty()) {
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
                      || (deployingInfernoMinefields() && (mf.getType() == Minefield.TYPE_INFERNO))
                      || (deployingEMPMinefields() && (mf.getType() == Minefield.TYPE_EMP))) {
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
                          clientgui.getFrame(), hex.depth());
                    smd.setVisible(true);

                    depth = smd.getDepth();
                }
                MineDensityDialog mfd = new MineDensityDialog(clientgui.getFrame());
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
                MineDensityDialog mfd = new MineDensityDialog(clientgui.getFrame());
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
                MineDensityDialog mfd = new MineDensityDialog(clientgui.getFrame());
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
                MineDensityDialog mfd = new MineDensityDialog(clientgui.getFrame());
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
                MineDensityDialog mfd = new MineDensityDialog(clientgui.getFrame());
                mfd.setVisible(true);

                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                      clientgui.getFrame());
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
            } else if (deployingEMPMinefields()) {
                // EMP mines cannot be placed in water
                if (sea) {
                    clientgui.doAlertDialog(Messages.getString("DeployMinefieldDisplay.IllegalPlacement"),
                          Messages.getString("DeployMinefieldDisplay.WaterPlacement"));
                    return;
                }
                // Get weight threshold setting from dialog
                EMPMineSettingDialog esd = new EMPMineSettingDialog(clientgui.getFrame());
                esd.setVisible(true);

                if (esd.getSetting() > 0) {
                    // Fixed density of 5 since EMP mines are one-use
                    mf = Minefield.createMinefield(coords, p.getId(),
                          Minefield.TYPE_EMP, 5, esd.getSetting());
                    p.setNbrMFEMP(p.getNbrMFEMP() - 1);

                    if (p.getNbrMFEMP() <= 0) {
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
        setEMPEnabled(p.getNbrMFEMP());
        setCarryableEnabled(p.getGroundObjectsToPlace().size());
    }

    @Override
    public void clear() {}

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent event) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (event.getType() != BoardViewEvent.BOARD_HEX_DRAGGED) {
            return;
        }

        // ignore buttons other than 1
        if (!isMyTurn() || ((event.getButton() != MouseEvent.BUTTON1))) {
            return;
        }

        // check for a deployment
        event.getBoardView().select(event.getCoords());
        deployMinefield(event);
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

        if (isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_your_turn"));
            clientgui.bingMyTurn();
        } else {
            String playerName = (e.getPlayer() != null) ? e.getPlayer().getName() : "Unknown";
            setStatusBarText(Messages.getString("DeployMinefieldDisplay.its_others_turn", playerName));
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && !clientgui.getClient().getGame().getPhase().isDeployMinefields()) {
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
        if (isIgnoringEvents() || !isMyTurn()) {
            return;
        }

        currentCommand = DeployMinefieldCommand.fromString(ev.getActionCommand());
    } // End public void actionPerformed(ActionEvent ev)

    @Override
    public void ready() {
        // Check if player has undeployed mines and warn them
        int undeployedMines = p.getNbrMFConventional() + p.getNbrMFCommand() + p.getNbrMFVibra()
              + p.getNbrMFActive() + p.getNbrMFInferno() + p.getNbrMFEMP();
        int undeployedCarryables = p.getGroundObjectsToPlace().size();

        if ((undeployedMines > 0) || (undeployedCarryables > 0)) {
            String message;
            if ((undeployedMines > 0) && (undeployedCarryables > 0)) {
                message = Messages.getString("DeployMinefieldDisplay.undeployedBoth",
                      undeployedMines, undeployedCarryables);
            } else if (undeployedMines > 0) {
                message = Messages.getString("DeployMinefieldDisplay.undeployedMines", undeployedMines);
            } else {
                message = Messages.getString("DeployMinefieldDisplay.undeployedCarryables", undeployedCarryables);
            }

            if (JOptionPane.showConfirmDialog(clientgui.getFrame(),
                  message,
                  Messages.getString("DeployMinefieldDisplay.undeployedTitle"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
        }

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

    private void setEMPEnabled(int nbr) {
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_EMP).setText(Messages.getString(
              "DeployMinefieldDisplay." + DeployMinefieldCommand.DEPLOY_MINE_EMP.getCmd(), nbr));
        buttons.get(DeployMinefieldCommand.DEPLOY_MINE_EMP).setEnabled(nbr > 0);
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
        clientgui.onAllBoardViews(bv -> bv.removeBoardViewListener(this));
    }
}
