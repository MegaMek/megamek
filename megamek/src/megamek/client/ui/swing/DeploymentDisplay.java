/*
 * Copyright (C) 2000-2006 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.*;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

public class DeploymentDisplay extends StatusBarPhaseDisplay {

    /**
     * This enumeration lists all the possible ActionCommands that can be
     * carried out during the deployment phase.  Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
     * @author arlith
     */
    public enum DeployCommand implements PhaseCommand {
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

        DeployCommand(String c) {
            cmd = c;
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
            return Messages.getString("DeploymentDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            String result = "";

            String msg_next= Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");

            switch (this) {
                case DEPLOY_NEXT:
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                    break;
                default:
                    break;
            }

            return result;
        }
    }

    protected Map<DeployCommand,MegamekButton> buttons;

    private int cen = Entity.NONE; // current entity number
    // is the shift key held?
    private boolean turnMode = false;
    private boolean assaultDropPreference = false;

    /** Creates and lays out a new deployment phase display for the specified client. */
    public DeploymentDisplay(ClientGUI clientgui) {
        super(clientgui);
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);
        setupStatusBar(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase"));

        setButtons();
        setButtonsTooltips();

        butDone.setText("<html><body>" + Messages.getString("DeploymentDisplay.Deploy") + "</body></html>");
        butDone.setEnabled(false);
        setupButtonPanel();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (DeployCommand.values().length * 1.25 + 0.5));
        for (DeployCommand cmd : DeployCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "DeploymentDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }
    @Override
    protected void setButtonsTooltips() {
        for (DeployCommand cmd : DeployCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "DeploymentDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        DeployCommand[] commands = DeployCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (DeployCommand cmd : commands) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /** Selects an entity for deployment. */
    public void selectEntity(int en) {
        // hmm, sometimes this gets called when there's no ready entities?
        if (clientgui.getClient().getGame().getEntity(en) == null) {
            disableButtons();
            setNextEnabled(true);
            LogManager.getLogger().error("DeploymentDisplay: Tried to select non-existent entity: " + en);
            return;
        }

        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }

        // FIXME: Hack alert: remove C3 sprites from earlier here, or we might crash when
        // trying to draw a c3 sprite belonging to the previously selected,
        // but not deployed entity. BoardView1 should take care of that itself.
        if (clientgui.getBoardView() != null) {
            clientgui.getBoardView().clearC3Networks();
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
                case Board.START_SW:
                    ce().setFacing(1);
                    ce().setSecondaryFacing(1);
                    break;
                case Board.START_SE:
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
                default:
                    ce().setFacing(0);
                    ce().setSecondaryFacing(0);
                    break;
            }
            boolean assaultDropOption = ce().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_ASSAULT_DROP);
            setAssaultDropEnabled(ce().canAssaultDrop() && assaultDropOption);
            if (!ce().canAssaultDrop() && assaultDropOption) {
                buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages.getString("DeploymentDisplay.AssaultDrop"));
                assaultDropPreference = false;
            }

            setLoadEnabled(!getLoadableEntities().isEmpty());
            setUnloadEnabled(!ce().getLoadedUnits().isEmpty());

            setNextEnabled(true);
            setRemoveEnabled(true);

            clientgui.getUnitDisplay().displayEntity(ce());
            clientgui.getUnitDisplay().showPanel("movement");
            clientgui.getBoardView().setWeaponFieldOfFire(ce().getFacing(), ce().getPosition());
        } else {
            disableButtons();
            setNextEnabled(true);
            clientgui.getBoardView().clearFieldOfFire();
        }
    }

    /** Enables relevant buttons and sets up for your turn. */
    private void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        selectEntity(clientgui.getClient().getFirstDeployableEntityNum());
        setNextEnabled(true);
        setRemoveEnabled(true);
        clientgui.getBoardView().markDeploymentHexesFor(ce());
    }

    /** Clears out old deployment data and disables relevant buttons. */
    private void endMyTurn() {
        final Game game = clientgui.getClient().getGame();
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isDeployment() && (null != next) && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().markDeploymentHexesFor(null);
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
    }

    /** Disables all buttons in the interface. */
    private void disableButtons() {
        for (DeployCommand cmd : DeployCommand.values()) {
            setButtonEnabled(cmd, false);
        }
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setAssaultDropEnabled(false);
    }

    private void setButtonEnabled(DeployCommand cmd, boolean enabled) {
        MegamekButton button = buttons.get(cmd);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    /** Sends a deployment to the server. */
    @Override
    public void ready() {
        final Game game = clientgui.getClient().getGame();
        final Entity en = ce();

        if ((en instanceof Dropship) && !en.isAirborne()) {
            ArrayList<Coords> crushedBuildingLocs = new ArrayList<>();
            ArrayList<Coords> secondaryPositions = new ArrayList<>();
            secondaryPositions.add(en.getPosition());
            for (int dir = 0; dir < 6; dir++) {
                secondaryPositions.add(en.getPosition().translated(dir));
            }
            for (Coords pos : secondaryPositions) {
                Building bld = game.getBoard().getBuildingAt(pos);
                if (bld != null) {
                    crushedBuildingLocs.add(pos);
                }
            }
            if (!crushedBuildingLocs.isEmpty()) {
                JOptionPane.showMessageDialog(clientgui,
                                Messages.getString("DeploymentDisplay.dropshipBuildingDeploy"),
                                Messages.getString("DeploymentDisplay.alertDialog.title"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Check nag for doomed planetary conditions
        String reason = game.getPlanetaryConditions().whyDoomed(en, game);
        if ((reason != null) && GUIP.getNagForDoomed()) {
            String title = Messages.getString("DeploymentDisplay.ConfirmDoomed.title");
            String body = Messages.getString("DeploymentDisplay.ConfirmDoomed.message", new Object[] {reason});
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIP.setNagForDoomed(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        disableButtons();

        int elevation = en.getElevation();
        // If elevation was set in lounge, try to preserve it
        // Server.processDeployment will adjust elevation, so we want to account for this
        Hex hex = game.getBoard().getHex(en.getPosition());
        if ((en instanceof VTOL) && (elevation >= 1)) {
            elevation = Math.max(0, elevation - (hex.ceiling() - hex.getLevel() + 1));
        }
        // Deploy grounded WiGEs on the roof of a building, and airborne at least one elevation above the roof.
        if ((en.getMovementMode() == EntityMovementMode.WIGE) && hex.containsTerrain(Terrains.BLDG_ELEV)) {
            int minElev = hex.terrainLevel(Terrains.BLDG_ELEV);
            if (elevation > 0) {
                minElev++;
            }
            elevation = Math.max(elevation, minElev);
        }

        clientgui.getClient().deploy(cen, en.getPosition(), en.getFacing(),
                elevation, en.getLoadedUnits(), assaultDropPreference);
        en.setDeployed(true);

        if (ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    /** Sends an entity removal to the server. */
    private void remove() {
        disableButtons();
        clientgui.getClient().sendDeleteEntity(cen);
        // Also remove units that are carried by the present unit
        for (Entity carried: clientgui.getClient().getGame().getEntity(cen).getLoadedUnits()) {
            clientgui.getClient().sendDeleteEntity(carried.getId());
        }
        cen = Entity.NONE;
    }

    /** Returns the current entity. */
    private Entity ce() {
        return clientgui.getClient().getGame().getEntity(cen);
    }

    public void die() {
        if (clientgui.getClient().isMyTurn()) {
            endMyTurn();
        }
        clientgui.getBoardView().markDeploymentHexesFor(null);
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

        final Game game = clientgui.getClient().getGame();
        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (game.getPhase().isSimultaneous(game)
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (game.getTurnIndex() != 0)) {
            return;
        }

        if (!game.getPhase().isDeployment()) {
            // ignore
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (clientgui.getClient().isMyTurn()) {
            if (cen == Entity.NONE) {
                beginMyTurn();
                clientgui.bingMyTurn();
            }
            setStatusBarText(Messages.getString("DeploymentDisplay.its_your_turn") + s);
        } else {
            endMyTurn();
            String playerName;

            if (e.getPlayer() != null) {
                playerName = e.getPlayer().getName();
            } else {
                playerName = "Unknown";
            }

            setStatusBarText(Messages.getString("DeploymentDisplay.its_others_turn", playerName) + s);
            clientgui.bingOthersTurn();
        }

    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        clientgui.getBoardView().markDeploymentHexesFor(null);

       // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            endMyTurn();
        }
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase().isDeployment()) {
            setStatusBarText(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase"));
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
        if (!clientgui.getClient().isMyTurn() || (ce() == null) || ((b.getButton() != MouseEvent.BUTTON1))) {
            return;
        }

        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0)
                || ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }

        // check for shifty goodness
        boolean shiftheld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;

        // check for a deployment
        Coords moveto = b.getCoords();
        final Board board = clientgui.getClient().getGame().getBoard();
        final Game game = clientgui.getClient().getGame();
        final Hex deployhex = board.getHex(moveto);
        final Building bldg = board.getBuildingAt(moveto);
        boolean isAero = ce().isAero();
        boolean isVTOL = ce() instanceof VTOL;
        boolean isWiGE = ce().getMovementMode().equals(EntityMovementMode.WIGE);
        boolean isTankOnPavement = ce().hasETypeFlag(Entity.ETYPE_TANK)
                && !ce().hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT)
                && !ce().isNaval()
                && deployhex.containsAnyTerrainOf(Terrains.PAVEMENT, Terrains.ROAD, Terrains.BRIDGE_ELEV);
        String title, msg;
        if ((ce().getPosition() != null) && (shiftheld || turnMode)) { // turn
            ce().setFacing(ce().getPosition().direction(moveto));
            ce().setSecondaryFacing(ce().getFacing());
            clientgui.getBoardView().redrawEntity(ce());
            clientgui.getBoardView().setWeaponFieldOfFire(ce().getFacing(), ce().getPosition());
            turnMode = false;
        } else if (ce().isBoardProhibited(board.getType())) {
            // check if this type of unit can be on the given type of map
            title = Messages.getString("DeploymentDisplay.alertDialog.title");
            msg = Messages.getString("DeploymentDisplay.wrongMapType", ce().getShortName(), Board.getTypeName(board.getType()));
            JOptionPane.showMessageDialog(clientgui, msg, title, JOptionPane.WARNING_MESSAGE);
            return;
        } else if (!(board.isLegalDeployment(moveto, ce()) || assaultDropPreference)
                || (ce().isLocationProhibited(moveto) && !isTankOnPavement)) {
            msg = Messages.getString("DeploymentDisplay.cantDeployInto", ce().getShortName(), moveto.getBoardNum());
            title = Messages.getString("DeploymentDisplay.alertDialog.title");
            JOptionPane.showMessageDialog(clientgui.frame, msg, title, JOptionPane.ERROR_MESSAGE);
            return;
        } else if (isAero && board.inAtmosphere() && (ce().getElevation() <= board.getHex(moveto).ceiling(true))) {
            // Ensure aeros don't end up at lower elevation than the current hex
            title = Messages.getString("DeploymentDisplay.alertDialog.title");
            msg = Messages.getString("DeploymentDisplay.elevationTooLow", ce().getShortName(), moveto.getBoardNum());
            JOptionPane.showMessageDialog(clientgui.frame, msg, title, JOptionPane.ERROR_MESSAGE);
            return;
        } else if ((Compute.stackingViolation(game, ce().getId(), moveto) != null) && (bldg == null)) {
            // check if deployed unit violates stacking
            return;
        } else {
            // check for buildings and if found ask what level they want to deploy at
            if ((null != bldg) && !isAero && !isVTOL && !isWiGE) {
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
            } else if (!isAero && !isWiGE) {
                // hovers and naval units go on the surface
                if ((ce().getMovementMode() == EntityMovementMode.NAVAL)
                        || (ce().getMovementMode() == EntityMovementMode.SUBMARINE)
                        || (ce().getMovementMode() == EntityMovementMode.HYDROFOIL)
                        || (ce().getMovementMode() == EntityMovementMode.HOVER)) {
                    ce().setElevation(0);
                } else if (isVTOL) {
                    // VTOLs go to elevation 1... unless set in the Lounge.
                    // or if mechanized BA, since VTOL movement is then illegal
                    if ((ce().getElevation() < 1) && (ce().getExternalUnits().size() <= 0)) {
                        ce().setElevation(1);
                    }
                } else {
                    // everything else goes to elevation 0, or on the floor of a
                    // water hex, except non-mechanized SCUBA infantry, which have a max depth of 2.
                    if (deployhex.containsTerrain(Terrains.WATER) && (ce() instanceof Infantry) && ((Infantry) ce()).isNonMechSCUBA()) {
                        ce().setElevation(Math.max(deployhex.floor() - deployhex.getLevel(), -2));
                    } else {
                        ce().setElevation(deployhex.floor() - deployhex.getLevel());
                    }
                }
            }
            ce().setPosition(moveto);

            clientgui.getBoardView().redrawEntity(ce());
            clientgui.getBoardView().setWeaponFieldOfFire(ce().getFacing(), moveto);
            clientgui.getBoardView().repaint();
            butDone.setEnabled(true);
        }
        if (!shiftheld) {
            clientgui.getBoardView().select(moveto);
        }
    }

    private boolean processBuildingDeploy(Coords moveto) {
        final Board board = clientgui.getClient().getGame().getBoard();
        final Game game = clientgui.getClient().getGame();

        int height = board.getHex(moveto).terrainLevel(Terrains.BLDG_ELEV);
        if (ce().getMovementMode() == EntityMovementMode.WIGE) {
            //TODO: Something seems to be missing here
        }
        ArrayList<String> floorNames = new ArrayList<>(height + 1);
        ArrayList<Integer> floorValues = new ArrayList<>(height + 1);

        if (Compute.stackingViolation(game, ce(), 0, moveto, null) == null) {
            floorNames.add(Messages.getString("DeploymentDisplay.ground"));
            floorValues.add(0);
        }

        for (int loop = 1; loop < height; loop++) {
            if (Compute.stackingViolation(game, ce(), loop, moveto, null) == null) {
                floorNames.add(Messages.getString("DeploymentDisplay.floor") + loop);
                floorValues.add(loop);
            }
        }
        if (Compute.stackingViolation(game, ce(), height, moveto, null) == null) {
            floorNames.add(Messages.getString("DeploymentDisplay.top"));
            floorValues.add(height);
        }

        // No valid floors to deploy on
        if (floorNames.size() < 1) {
            String msg = Messages.getString("DeploymentDisplay.cantDeployInto", ce().getShortName(), moveto.getBoardNum());
            String title = Messages.getString("DeploymentDisplay.alertDialog.title");
            JOptionPane.showMessageDialog(clientgui.frame, msg, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String msg = Messages.getString("DeploymentDisplay.floorsDialog.message", ce().getShortName());
        String title = Messages.getString("DeploymentDisplay.floorsDialog.title");
        String input = (String) JOptionPane.showInputDialog(clientgui, msg, title, JOptionPane.QUESTION_MESSAGE, null,
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
        final Board board = clientgui.getClient().getGame().getBoard();
        final Hex deployhex = board.getHex(moveto);

        int height = board.getHex(moveto).terrainLevel(Terrains.BRIDGE_ELEV);
        List<String> floors = new ArrayList<>(2);
        if (!ce().isLocationProhibited(moveto)) {
            floors.add(Messages.getString("DeploymentDisplay.belowbridge"));
        }

        // ships can't deploy to the top of a bridge
        if (!ce().isNaval()) {
            floors.add(Messages.getString("DeploymentDisplay.topbridge"));
        }

        String title = Messages.getString("DeploymentDisplay.bridgeDialog.title");
        String msg = Messages.getString("DeploymentDisplay.bridgeDialog.message", ce().getShortName());
        String input = (String) JOptionPane.showInputDialog(clientgui, msg,
                title, JOptionPane.QUESTION_MESSAGE, null, floors.toArray(), null);
        if (input != null) {
            if (input.equals(Messages.getString("DeploymentDisplay.topbridge"))) {
                ce().setElevation(height);
            } else {
                if (ce().isNaval() && (ce().getMovementMode() != EntityMovementMode.SUBMARINE)) {
                    ce().setElevation(0);
                } else {
                    ce().setElevation(deployhex.floor() - deployhex.getLevel());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent evt) {
        final Client client = clientgui.getClient();
        final String actionCmd = evt.getActionCommand();
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (!client.isMyTurn()) {

        } else if (actionCmd.equals(DeployCommand.DEPLOY_NEXT.getCmd())) {
            if (ce() != null) {
                ce().setPosition(null);
                clientgui.getBoardView().redrawEntity(ce());
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
            if (!choices.isEmpty()) {
                // If we have multiple choices, display a selection dialog.
                Entity other = EntityChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                        Messages.getString("DeploymentDisplay.loadUnitDialog.message", ce().getShortName(), ce().getUnusedString()),
                        Messages.getString("DeploymentDisplay.loadUnitDialog.title"),
                        choices);

                if (!(other instanceof Infantry)) {
                    List<Integer> bayChoices = new ArrayList<>();
                    for (Transporter t : ce().getTransports()) {
                        if (t.canLoad(other) && (t instanceof Bay)) {
                            bayChoices.add(((Bay) t).getBayNumber());
                        }
                    }

                    if (bayChoices.size() > 1) {
                        String[] retVal = new String[bayChoices.size()];
                        int i = 0;
                        for (Integer bn : bayChoices) {
                            retVal[i++] = bn.toString() + " (Free Slots: " + (int) ce().getBayById(bn).getUnused() + ")";
                        }
                        String title = Messages.getString("DeploymentDisplay.loadUnitBayNumberDialog.title");
                        String msg = Messages.getString("DeploymentDisplay.loadUnitBayNumberDialog.message", ce().getShortName());
                        String bayString = (String) JOptionPane.showInputDialog(clientgui, msg, title,
                                JOptionPane.QUESTION_MESSAGE, null, retVal, null);
                        int bayNum = Integer.parseInt(bayString.substring(0, bayString.indexOf(" ")));
                        other.setTargetBay(bayNum);
                        // We need to update the entity here so that the server knows about our target bay
                        client.sendUpdateEntity(other);
                    } else if (other.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                        bayChoices = new ArrayList<>();
                        for (Transporter t : ce().getTransports()) {
                            if ((t instanceof ProtomechClampMount) && t.canLoad(other)) {
                                bayChoices.add(((ProtomechClampMount) t).isRear() ? 1 : 0);
                            }
                        }
                        if (bayChoices.size() > 1) {
                            String[] retVal = new String[bayChoices.size()];
                            int i = 0;
                            for (Integer bn : bayChoices) {
                                retVal[i++] = bn > 0 ?
                                        Messages.getString("MovementDisplay.loadProtoClampMountDialog.rear") :
                                        Messages.getString("MovementDisplay.loadProtoClampMountDialog.front");
                            }
                            String bayString = (String) JOptionPane.showInputDialog(clientgui,
                                    Messages.getString("MovementDisplay.loadProtoClampMountDialog.message", ce().getShortName()),
                                    Messages.getString("MovementDisplay.loadProtoClampMountDialog.title"),
                                    JOptionPane.QUESTION_MESSAGE, null, retVal, null);
                            other.setTargetBay(bayString.equals(Messages.getString("MovementDisplay.loadProtoClampMountDialog.front")) ? 0 : 1);
                            // We need to update the entity here so that the server knows about our target bay
                            clientgui.getClient().sendUpdateEntity(other);
                        } else {
                            other.setTargetBay(-1); // Safety set!
                        }
                    } else {
                        other.setTargetBay(-1); // Safety set!
                    }
                } else {
                    other.setTargetBay(-1); // Safety set!
                }

                // Please note, the Server may never get this load order.
                ce().load(other, false, other.getTargetBay());
                other.setTransportId(cen);
                clientgui.getUnitDisplay().displayEntity(ce());
                setUnloadEnabled(true);
            } else {
                JOptionPane.showMessageDialog(clientgui.frame,
                        Messages.getString("DeploymentDisplay.alertDialog1.message", ce().getShortName()),
                        Messages.getString("DeploymentDisplay.alertDialog1.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_UNLOAD.getCmd())) {
            // Do we have anyone to unload?
            Entity loader = ce();
            List<Entity> choices = loader.getLoadedUnits();
            if (!choices.isEmpty()) {

                Entity loaded = EntityChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                        Messages.getString("DeploymentDisplay.unloadUnitDialog.message", ce().getShortName(), ce().getUnusedString()),
                        Messages.getString("DeploymentDisplay.unloadUnitDialog.title"),
                        choices);

                if (loaded != null) {
                    if (loader.unload(loaded)) {
                        loaded.setTransportId(Entity.NONE);
                        loaded.newRound(clientgui.getClient().getGame().getRoundCount());
                        clientgui.getUnitDisplay().displayEntity(ce());
                        // Unit loaded in the lobby?  Server needs updating
                        if (loader.getLoadedKeepers().contains(loaded.getId())) {
                            Vector<Integer> lobbyLoaded = loader.getLoadedKeepers();
                            lobbyLoaded.removeElement(loaded.getId());
                            loader.setLoadedKeepers(lobbyLoaded);
                            client.sendDeploymentUnload(loader, loaded);
                            // Need to take turn for unloaded unit, so select it
                            selectEntity(loaded.getId());
                        }
                        setLoadEnabled(!getLoadableEntities().isEmpty());
                    } else {
                        LogManager.getLogger().error("Could not unload " + loaded.getShortName() + " from " + ce().getShortName());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(clientgui.frame,
                        Messages.getString("DeploymentDisplay.alertDialog2.message", ce().getShortName()),
                        Messages.getString("DeploymentDisplay.alertDialog2.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_REMOVE.getCmd())) {
            if (JOptionPane.showConfirmDialog(clientgui.frame,
                    Messages.getString("DeploymentDisplay.removeUnit", ce().getShortName()),
                    Messages.getString("DeploymentDisplay.removeTitle"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                remove();
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_ASSAULTDROP.getCmd())) {
            assaultDropPreference = !assaultDropPreference;
            if (assaultDropPreference) {
                buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages.getString("DeploymentDisplay.assaultDropOff"));
            } else {
                buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setText(Messages.getString("DeploymentDisplay.assaultDrop"));
            }
        }
    }

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
            clientgui.maybeShowUnitDisplay();
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
        clientgui.getBoardView().clearFieldOfFire();
        if (client.isMyTurn()) {
            if (client.getGame().getTurn().isValidEntity(e, client.getGame())) {
                if (ce() != null) {
                    ce().setPosition(null);
                    clientgui.getBoardView().redrawEntity(ce());
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
                    clientgui.getBoardView().centerOnHex(e.getPosition());
                }
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(e);
            if (e.isDeployed()) {
                clientgui.getBoardView().centerOnHex(e.getPosition());
            }
        }
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_NEXT.getCmd(), enabled);
    }

    private void setTurnEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_TURN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_TURN.getCmd(), enabled);
    }

    private void setLoadEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_LOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_LOAD.getCmd(), enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_UNLOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_UNLOAD.getCmd(), enabled);
    }

    private void setRemoveEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_REMOVE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_REMOVE.getCmd(), enabled);
    }

    private void setAssaultDropEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_ASSAULTDROP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_ASSAULTDROP.getCmd(), enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        die();
    }

    /** Returns a list of the entities that can be loaded into the currently selected entity. */
    private List<Entity> getLoadableEntities() {
        ArrayList<Entity> choices = new ArrayList<>();
        // If current entity is null, nothing to do
        if (ce() == null) {
            return choices;
        }
        List<Entity> entities = clientgui.getClient().getGame().getEntitiesVector();
        for (Entity other : entities) {
            if (other.isSelectableThisTurn() && ce().canLoad(other, false)
                    // We can't depend on the transport id to be set because we sent a server update
                    // before loading on the client side, and the loaded unit may have been reset
                    // by the resulting update from the server.
                    && !ce().getLoadedUnits().contains(other)
                    // If you want to load a trailer into a DropShip or large support vehicle, do it in the lobby
                    // The 'load' button should not allow trailers - that's what 'tow' is for.
                    && !other.isTrailer()) {
                choices.add(other);
            }
        }
        return choices;
    }
}
