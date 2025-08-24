/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.uiLightViolet;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.containers.PlayerIDAndList;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

public class SelectArtyAutoHitHexDisplay extends StatusBarPhaseDisplay {
    @Serial
    private static final long serialVersionUID = -4948184589134809323L;

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the select arty auto hit
     * phase.  Each command has a string for the command plus a flag that determines what unit type it is appropriate
     * for.
     *
     * @author arlith
     */
    public enum ArtyAutoHitCommand implements PhaseCommand {
        SET_HIT_HEX("setAutoHitHex");

        final String cmd;

        private int priority;

        ArtyAutoHitCommand(String c) {
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
        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public String toString() {
            return Messages.getString("SelectArtyAutoHitHexDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            return "";
        }
    }

    private final ClientGUI clientgui;
    private Map<PhaseCommand, MegaMekButton> buttons;

    private Player player;
    private final Map<BoardLocation, SpecialHexDisplay> plannedAutoHits = new HashMap<>();
    private int allowedNumberOfHexes;

    /**
     * Creates and lays out a new select designated hex phase display for the specified clientGUI.getClient().
     */
    public SelectArtyAutoHitHexDisplay(ClientGUI clientgui) {
        super(clientgui);
        this.clientgui = clientgui;
        player = clientgui.getClient().getLocalPlayer();
        game().addGameListener(this);

        setupStatusBar(Messages.getString("SelectArtyAutoHitHexDisplay.waitingArtillery"));
        setButtons();
        setButtonsTooltips();
        butDone.setText(Messages.getString("SelectArtyAutoHitHexDisplay.Done"));
        butDone.setEnabled(false);
        setupButtonPanel();
        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (ArtyAutoHitCommand.values().length * 1.25 + 0.5));
        for (ArtyAutoHitCommand cmd : ArtyAutoHitCommand.values()) {
            String title = Messages.getString("SelectArtyAutoHitHexDisplay." + cmd.getCmd());
            MegaMekButton newButton = new MegaMekButton(title,
                  SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (ArtyAutoHitCommand cmd : ArtyAutoHitCommand.values()) {
            String ttKey = "SelectArtyAutoHitHexDisplay." + cmd.getCmd() + ".tooltip";
            String tt = cmd.getHotKeyDesc();
            if (!tt.isEmpty()) {
                String title = Messages.getString("SelectArtyAutoHitHexDisplay." + cmd.getCmd());
                tt = UIUtil.fontHTML(uiLightViolet()) + title + ": " + tt + "</FONT>";
                tt += "<BR>";
            }
            if (Messages.keyExists(ttKey)) {
                String msg_key = Messages.getString(ttKey);
                tt += msg_key;
            }
            if (!tt.isEmpty()) {
                String b = "<BODY>" + tt + "</BODY>";
                String h = "<HTML>" + b + "</HTML>";
                buttons.get(cmd).setToolTipText(h);
            }
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        ArtyAutoHitCommand[] commands = ArtyAutoHitCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (ArtyAutoHitCommand cmd : commands) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        // Make sure we've got the correct local player
        player = clientgui.getClient().getLocalPlayer();
        // By default, we should get 5 hexes per 4 map sheets (4 map sheets is 16*17*4 = 1088 hexes)
        Board board = game().getBoard();
        int preDesignateArea = game().getOptions().intOption(OptionsConstants.ADVANCED_COMBAT_MAP_AREA_PREDESIGNATE);
        int hexesPer = game().getOptions().intOption(OptionsConstants.ADVANCED_COMBAT_NUM_HEXES_PREDESIGNATE);
        double mapArea = board.getWidth() * board.getHeight();
        allowedNumberOfHexes = (int) Math.ceil(mapArea / preDesignateArea) * hexesPer;
        plannedAutoHits.clear();
        setArtyEnabled(allowedNumberOfHexes);
        butDone.setEnabled(true);
        startTimer();
    }

    /**
     * Clears out old data and disables relevant buttons.
     */
    private void endMyTurn() {
        stopTimer();
        disableButtons();
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setArtyEnabled(0);
        butDone.setEnabled(false);
    }

    private void addArtyAutoHitHex(BoardLocation location) {
        if (!game().hasBoardLocation(location)) {
            return;
        }

        if (!plannedAutoHits.containsKey(location)) {
            if (!isValidArtyAutoLocation(location)) {
                showInvalidLocationMessage();
                return;
            } else if (plannedAutoHits.size() >= allowedNumberOfHexes) {
                showTooManyTargetsMessage();
                return;
            }
            player.addArtyAutoHitHex(location);
            var autoHitIcon = SpecialHexDisplay.createArtyAutoHit(player);
            game().getBoard(location).addSpecialHexDisplay(location.coords(), autoHitIcon, true);
            plannedAutoHits.put(location, autoHitIcon);
        } else {
            SpecialHexDisplay autoHitIcon = plannedAutoHits.get(location);
            player.removeArtyAutoHitHex(location);
            game().getBoard(location).removeSpecialHexDisplay(location.coords(), autoHitIcon, true);
            plannedAutoHits.remove(location);
        }
        setArtyEnabled(allowedNumberOfHexes - plannedAutoHits.size());
        clientgui.boardViews().forEach(IBoardView::refreshDisplayables);
    }

    private boolean isValidArtyAutoLocation(BoardLocation location) {
        return game().isOnGroundMap(location);
    }

    private void showTooManyTargetsMessage() {
        JOptionPane.showMessageDialog(clientgui.getFrame(),
              Messages.getString("SelectArtyAutoHitHexDisplay.TooManyTargets.message"),
              Messages.getString("SelectArtyAutoHitHexDisplay.TooManyTargets.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }

    private void showInvalidLocationMessage() {
        JOptionPane.showMessageDialog(clientgui.getFrame(),
              Messages.getString("SelectArtyAutoHitHexDisplay.NotAllowed.message"),
              Messages.getString("SelectArtyAutoHitHexDisplay.NotAllowed.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }

    //
    // BoardViewListener
    //
    @Override
    public void hexMoused(BoardViewEvent event) {
        if (isIgnoringEvents() || !isMyTurn() || (event.getType() != BoardViewEvent.BOARD_HEX_CLICKED) ||
              (event.getButton() != MouseEvent.BUTTON1)) {
            return;
        }

        addArtyAutoHitHex(event.getBoardLocation());
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        endMyTurn();

        if (isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.its_your_turn"));
            clientgui.bingMyTurn();
        } else {
            String playerName = (e.getPlayer() != null) ? e.getPlayer().getName() : "Unknown";
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.its_others_turn", playerName));
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(final GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        if (game().getPhase().isSetArtilleryAutoHitHexes()) {
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.waitingMinefieldPhase"));
        } else if (isMyTurn()) {
            endMyTurn();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {}

    @Override
    public void clear() {
        plannedAutoHits.forEach((location, shd) ->
              game().getBoard(location).removeSpecialHexDisplay(location.coords(), shd, true));
        plannedAutoHits.clear();
        player.removeArtyAutoHitHexes();
        setArtyEnabled(allowedNumberOfHexes);
    }

    @Override
    public void ready() {
        endMyTurn();
        PlayerIDAndList<BoardLocation> finalAutoHits = new PlayerIDAndList<>();
        finalAutoHits.setPlayerID(player.getId());
        finalAutoHits.addAll(plannedAutoHits.keySet());
        plannedAutoHits.clear();
        clientgui.getClient().sendArtyAutoHitHexes(finalAutoHits);
        clientgui.getClient().sendPlayerInfo();
    }

    private void setArtyEnabled(int remainingHexes) {
        buttons.get(ArtyAutoHitCommand.SET_HIT_HEX).setText(
              Messages.getString("SelectArtyAutoHitHexDisplay." + ArtyAutoHitCommand.SET_HIT_HEX.getCmd(),
                    remainingHexes));
        buttons.get(ArtyAutoHitCommand.SET_HIT_HEX).setEnabled(remainingHexes > 0);
    }

    @Override
    public void removeAllListeners() {
        game().removeGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.removeBoardViewListener(this));
    }

    private void toggleShowDeployment() {
        clientgui.onAllBoardViews(BoardView::toggleShowDeployment);
    }

    private void registerKeyCommands() {
        clientgui.controller.registerCommandAction(KeyCommandBind.AUTO_ARTY_DEPLOYMENT_ZONE,
              this::shouldReceiveShowDeployment, this::toggleShowDeployment, () -> {});
    }

    private Game game() {
        return clientgui.getClient().getGame();
    }

    public boolean shouldReceiveShowDeployment() {
        return !clientgui.isChatBoxActive() && !isIgnoringEvents() && isVisible();
    }
}
