/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

/**
 * The Victory Setup phase display: each player in turn places and configures their control points directly on the
 * real game board, before artillery is pre-sighted and minefields are laid - objectives first, because both of
 * those decisions depend on knowing where the objectives are. Clicking an empty hex places a new control point and
 * opens its properties (radius, victory points, scoring scheme); clicking one of the player's own points reopens
 * the properties, where the point can also be removed. Points assigned before the game began - by MekHQ or a
 * scenario file - arrive already placed and can be adjusted here the same way.
 */
public class VictorySetupDisplay extends StatusBarPhaseDisplay {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger VICTORY_HEX_LOGGER = MMLogger.create("megamek.feature.VictoryHex");

    /** The one phase command: informational, the real interaction is clicking the board. */
    public enum VictorySetupCommand implements PhaseCommand {
        PLACE_POINT("placePoint");

        final String cmd;
        private int priority;

        VictorySetupCommand(String cmd) {
            this.cmd = cmd;
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
            return Messages.getString("VictorySetupDisplay." + getCmd());
        }
    }

    private final ClientGUI clientgui;
    private Map<PhaseCommand, MegaMekButton> buttons;
    private Player player;

    public VictorySetupDisplay(ClientGUI clientgui) {
        super(clientgui);
        this.clientgui = clientgui;
        player = clientgui.getClient().getLocalPlayer();
        game().addGameListener(this);

        setupStatusBar(Messages.getString("VictorySetupDisplay.waitingVictorySetup"));
        setButtons();
        setButtonsTooltips();
        butDone.setText(Messages.getString("VictorySetupDisplay.Done"));
        butDone.setEnabled(false);
        setupButtonPanel();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>();
        for (VictorySetupCommand command : VictorySetupCommand.values()) {
            String title = Messages.getString("VictorySetupDisplay." + command.getCmd());
            MegaMekButton newButton = new MegaMekButton(title,
                  SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            newButton.addActionListener(this);
            newButton.setActionCommand(command.getCmd());
            newButton.setEnabled(false);
            buttons.put(command, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (VictorySetupCommand command : VictorySetupCommand.values()) {
            String tooltipKey = "VictorySetupDisplay." + command.getCmd() + ".tooltip";
            if (Messages.keyExists(tooltipKey)) {
                buttons.get(command).setToolTipText(Messages.getString(tooltipKey));
            }
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        return new ArrayList<>(buttons.values());
    }

    private void beginMyTurn() {
        player = clientgui.getClient().getLocalPlayer();
        buttons.get(VictorySetupCommand.PLACE_POINT).setEnabled(true);
        butDone.setEnabled(true);
        startTimer();
    }

    private void endMyTurn() {
        stopTimer();
        buttons.get(VictorySetupCommand.PLACE_POINT).setEnabled(false);
        butDone.setEnabled(false);
    }

    /**
     * Places a new control point at the clicked hex, or reopens the properties of the player's own point there.
     * All changes stay local until the player is done; the Done click sends the ground objects to the server.
     */
    private void handleHexClick(Coords coords) {
        ObjectiveMarker existingMarker = findMarkerAt(coords);
        if (existingMarker != null) {
            if (existingMarker.getOwnerId() != player.getId()) {
                clientgui.addToast(ToastLevel.WARNING,
                      Messages.getString("VictorySetupDisplay.notYourPoint"));
                return;
            }
            editMarker(coords, existingMarker);
            return;
        }
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(Messages.getString("VictoryHex.name", coords.getBoardNum()));
        marker.setOwnerId(player.getId());
        game().placeGroundObject(coords, marker);
        VICTORY_HEX_LOGGER.info("[VictoryHex] {} placed in the Victory Setup phase", coords.getBoardNum());
        refreshBoard();
        // a new point immediately opens its properties: click a hex, set the variables
        editMarker(coords, marker);
    }

    private void editMarker(Coords coords, ObjectiveMarker marker) {
        VictoryHexPropertiesPane.Result result = VictoryHexPropertiesPane.edit(clientgui.getFrame(), marker);
        if (result == VictoryHexPropertiesPane.Result.REMOVED) {
            game().removeGroundObject(coords, marker);
            VICTORY_HEX_LOGGER.info("[VictoryHex] {} removed in the Victory Setup phase", coords.getBoardNum());
        }
        refreshBoard();
    }

    private @Nullable ObjectiveMarker findMarkerAt(Coords coords) {
        for (ICarryable groundObject : game().getGroundObjects(coords)) {
            if (groundObject instanceof ObjectiveMarker marker) {
                return marker;
            }
        }
        return null;
    }

    private void refreshBoard() {
        clientgui.showGroundObjects(game().getGroundObjects());
    }

    @Override
    public void hexMoused(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }
        boolean isLeftClickOnHex = (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED)
              && (event.getButton() == MouseEvent.BUTTON1);
        if (!isLeftClickOnHex) {
            // pointer movement and right clicks are not attempts to place anything; they pass quietly
            return;
        }
        if (!isMyTurn()) {
            explainClickOutsideMyTurn(event.getCoords());
            return;
        }
        BoardLocation location = event.getBoardLocation();
        // control points live on the ground map, like the ground objects they are
        if (!game().hasBoardLocation(location) || !game().isOnGroundMap(location)) {
            clientgui.addToast(ToastLevel.ERROR, Messages.getString("VictorySetupDisplay.notGroundMap"));
            VICTORY_HEX_LOGGER.debug("[VictoryHex] click ignored: {} is not a hex on the ground map",
                  location);
            return;
        }
        handleHexClick(location.coords());
    }

    /**
     * Tells the player why their click did nothing, rather than swallowing it. A player who owns no units
     * is marked an observer when the phase begins and never receives a turn in it at all, so without this
     * their clicks are silent and neither the screen nor the log explains why.
     *
     * @param coords The hex that was clicked, for the diagnostic log, or {@code null} when the click
     *               carried no hex
     */
    private void explainClickOutsideMyTurn(@Nullable Coords coords) {
        Player localPlayer = clientgui.getClient().getLocalPlayer();
        boolean isObserver = (localPlayer != null) && localPlayer.isObserver();
        String messageKey = isObserver
              ? "VictorySetupDisplay.observerCannotPlace"
              : "VictorySetupDisplay.notYourTurn";
        clientgui.addToast(ToastLevel.WARNING, Messages.getString(messageKey));
        VICTORY_HEX_LOGGER.debug("[VictoryHex] click on {} ignored for {}: {}",
              (coords == null) ? "no hex" : coords.getBoardNum(),
              (localPlayer == null) ? "unknown player" : localPlayer.getName(),
              isObserver
                    ? "observer - owns no units, so takes no turn in this phase"
                    : "not this player's turn yet");
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent event) {
        if (isIgnoringEvents()) {
            return;
        }
        endMyTurn();
        if (isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("VictorySetupDisplay.its_your_turn"));
            clientgui.bingMyTurn();
        } else {
            String playerName = (event.getPlayer() != null) ? event.getPlayer().getName() : "Unknown";
            setStatusBarText(Messages.getString("VictorySetupDisplay.its_others_turn", playerName));
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent event) {
        if (isIgnoringEvents()) {
            return;
        }
        if (game().getPhase().isVictorySetup()) {
            setStatusBarText(Messages.getString("VictorySetupDisplay.waitingVictorySetup"));
        } else if (isMyTurn()) {
            endMyTurn();
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        // the one button is informational; the interaction is clicking the board
    }

    @Override
    public void clear() {
        // nothing to clear: placements apply immediately and are edited or removed in place
    }

    @Override
    public void ready() {
        if (!confirmLeavingPointsUnplaced()) {
            return;
        }
        endMyTurn();
        // sending the ground objects is this phase's turn action: the server stores them, rebroadcasts,
        // and ends the turn
        clientgui.getClient().sendDeployGroundObjects(game().getGroundObjects());
    }

    /**
     * Asks before ending a turn that placed nothing, the way the minefield phase asks about undeployed
     * mines. The phase is easy to click straight through, and a player who does gets a game with victory
     * points enabled and nothing on the board to score them.
     *
     * @return {@code true} to go ahead and end the turn, {@code false} to stay in the phase
     */
    private boolean confirmLeavingPointsUnplaced() {
        int pointsOnBoard = 0;
        for (List<ICarryable> hexObjects : game().getGroundObjects().values()) {
            for (ICarryable groundObject : hexObjects) {
                if ((groundObject instanceof ObjectiveMarker marker) && (marker.getOwnerId() == player.getId())) {
                    pointsOnBoard++;
                }
            }
        }
        int pointsStillToPlace = player.getGroundObjectsToPlace().size();
        if ((pointsOnBoard > 0) && (pointsStillToPlace == 0)) {
            return true;
        }
        String message = (pointsStillToPlace > 0)
              ? Messages.getString("VictorySetupDisplay.unplacedPoints", pointsStillToPlace)
              : Messages.getString("VictorySetupDisplay.noPointsPlaced");
        int choice = JOptionPane.showConfirmDialog(clientgui.getFrame(), message,
              Messages.getString("VictorySetupDisplay.unplacedTitle"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        boolean isGoingAhead = choice == JOptionPane.YES_OPTION;
        if (!isGoingAhead) {
            VICTORY_HEX_LOGGER.debug("[VictoryHex] {} stayed in the phase rather than end the turn with "
                  + "{} point(s) placed and {} still to place", player.getName(), pointsOnBoard,
                  pointsStillToPlace);
        }
        return isGoingAhead;
    }

    @Override
    public void removeAllListeners() {
        game().removeGameListener(this);
        clientgui.boardViews().forEach(boardView -> boardView.removeBoardViewListener(this));
    }

    private Game game() {
        return clientgui.getClient().getGame();
    }
}
