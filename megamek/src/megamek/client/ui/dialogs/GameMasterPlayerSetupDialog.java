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
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.interfaces.IStartingPositions;
import megamek.logging.MMLogger;

/**
 * Lets a Game Master put a player on a team and say which edge their units arrive from.
 *
 * <p>This is what turns somebody watching a game into somebody playing it. A player who joins a game already in
 * progress arrives on no team and with no deployment zone, and neither can be fixed from the lobby because the lobby
 * is long gone. Without a team they are left out of the turn order entirely, so their units can never move; without a
 * zone their units may walk on anywhere at all, including behind the enemy.</p>
 *
 * <p>Both choosers start on what the chosen player already has, so changing one does not disturb the other. Nothing
 * is sent for a value that was not changed.</p>
 */
public class GameMasterPlayerSetupDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(GameMasterPlayerSetupDialog.class);

    /** Unscaled padding around a row; scaled through {@link UIUtil#scaleForGUI(int)} when used. */
    private static final int ROW_PADDING = 4;

    private final ClientGUI clientGUI;
    private final JComboBox<PlayerChoice> playerChooser = new JComboBox<>();
    private final JComboBox<TeamChoice> teamChooser = new JComboBox<>();
    private final JComboBox<ZoneChoice> zoneChooser = new JComboBox<>();

    /** Says whether what is on screen can be applied, and why not when it cannot. */
    private final JLabel statusLabel = new JLabel();

    private final JButton applyButton = new JButton(Messages.getString("GameMasterPlayerSetupDialog.apply"));

    /**
     * The two ways of giving the player a force, offered here so the job is done in one place and in an order that
     * works: they stay switched off until the setup has been applied, because units handed to somebody who cannot
     * deploy yet are stranded rather than delayed.
     */
    private final JButton reinforceFromFileButton =
          new JButton(Messages.getString("CommonMenuBar.fileUnitsReinforce"));
    private final JButton reinforceFromGeneratorButton =
          new JButton(Messages.getString("CommonMenuBar.fileUnitsReinforceRAT"));

    /** The player whose setup has been applied, so reinforcing them is now safe. Cleared by choosing another. */
    private int playerReadyForUnits = Player.PLAYER_NONE;

    /** One player offered in the chooser, named rather than numbered. */
    private record PlayerChoice(int playerId, String playerName) {
        @Override
        public String toString() {
            return playerName;
        }
    }

    /**
     * One team offered in the chooser, plus the choice of starting a fresh one.
     *
     * <p>{@link #NEW_TEAM} is not a team; it stands for "whichever team nobody is using yet" and is worked out when
     * Apply is pressed, so a gamemaster splitting people up does not have to remember which numbers are taken.</p>
     */
    private record TeamChoice(int teamId, List<String> membersOnIt) {
        /** Stands for the first team nobody is on, resolved when the change is applied. */
        private static final int NEW_TEAM = Integer.MIN_VALUE;

        /** @return a choice for the given team, with nobody listed on it */
        private static TeamChoice of(int teamId) {
            return new TeamChoice(teamId, List.of());
        }

        /** A choice is the same team whoever happened to be on it when the label was built. */
        @Override
        public boolean equals(Object other) {
            return (other instanceof TeamChoice otherChoice) && (otherChoice.teamId == teamId);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(teamId);
        }

        @Override
        public String toString() {
            if (teamId == NEW_TEAM) {
                return Messages.getString("GameMasterPlayerSetupDialog.newTeam");
            }
            String name = (teamId == Player.TEAM_UNASSIGNED)
                  ? Messages.getString("GameMasterPlayerSetupDialog.noTeam")
                  : Player.TEAM_NAMES[teamId];
            return membersOnIt.isEmpty() ? name : name + " - " + String.join(", ", membersOnIt);
        }
    }

    /** One deployment zone offered in the chooser, named the way the lobby names it. */
    private record ZoneChoice(int zoneId, String zoneName) {
        @Override
        public String toString() {
            return zoneName;
        }
    }

    /**
     * Opens the dialog on the given player.
     *
     * @param parent    The frame to open over
     * @param clientGUI The client the changes are sent through
     * @param player    The player to start on, or {@code null} to start on the first in the game
     */
    public GameMasterPlayerSetupDialog(JFrame parent, ClientGUI clientGUI, @Nullable Player player) {
        super(parent, Messages.getString("GameMasterPlayerSetupDialog.title"), false);
        this.clientGUI = clientGUI;

        buildUI(parent);
        if (player != null) {
            playerChooser.setSelectedItem(new PlayerChoice(player.getId(), player.getName()));
        }
        loadFromChosenPlayer();
    }

    private void buildUI(JFrame parent) {
        List<String> offered = new ArrayList<>();
        for (Player player : clientGUI.getClient().getGame().getPlayersList()) {
            if (!mayBeSetUp(player)) {
                continue;
            }
            playerChooser.addItem(new PlayerChoice(player.getId(), player.getName()));
            offered.add(player.getName());
        }
        LOGGER.info("[GMPlayerSetup] offering to set up {} player(s): {}", offered.size(), offered);
        // the teams that exist, plus the one a player sits on before they are given any: a gamemaster taking someone
        // back out of the game needs the same list as one putting them in
        teamChooser.addItem(TeamChoice.of(Player.TEAM_UNASSIGNED));
        for (int teamId = 0; teamId < Player.TEAM_NAMES.length; teamId++) {
            teamChooser.addItem(new TeamChoice(teamId, membersOf(teamId)));
        }
        if (firstUnusedTeam() != Player.TEAM_UNASSIGNED) {
            teamChooser.addItem(TeamChoice.of(TeamChoice.NEW_TEAM));
        }
        for (int zone = 0; zone < IStartingPositions.START_LOCATION_NAMES.length; zone++) {
            zoneChooser.addItem(new ZoneChoice(zone, IStartingPositions.START_LOCATION_NAMES[zone]));
        }
        playerChooser.addActionListener(event -> loadFromChosenPlayer());
        teamChooser.addActionListener(event -> refreshLegality());

        int rowPadding = UIUtil.scaleForGUI(ROW_PADDING);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(rowPadding, rowPadding, rowPadding, rowPadding));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(rowPadding, rowPadding, rowPadding, rowPadding);
        constraints.anchor = GridBagConstraints.WEST;
        addRow(fields, constraints, 0, "GameMasterPlayerSetupDialog.player", playerChooser);
        addRow(fields, constraints, 1, "GameMasterPlayerSetupDialog.teamLabel", teamChooser);
        addRow(fields, constraints, 2, "GameMasterPlayerSetupDialog.zoneLabel", zoneChooser);

        // the two settings do not take effect at the same moment, and a gamemaster who is not told that reads the
        // delay as the dialog having done nothing
        constraints.gridy = 3;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        fields.add(new JLabel(Messages.getString("GameMasterPlayerSetupDialog.whenApplied")), constraints);
        constraints.gridy = 4;
        fields.add(statusLabel, constraints);
        constraints.gridwidth = 1;

        getContentPane().setLayout(new BorderLayout());
        JPanel below = new JPanel(new BorderLayout());
        below.add(reinforcePanel(), BorderLayout.PAGE_START);
        below.add(buttonPanel(), BorderLayout.PAGE_END);

        getContentPane().add(fields, BorderLayout.CENTER);
        getContentPane().add(below, BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }

    /** Adds one labelled row to the form. */
    private static void addRow(JPanel fields, GridBagConstraints constraints, int row, String labelKey,
          JComponent control) {
        constraints.gridy = row;
        constraints.gridx = 0;
        fields.add(new JLabel(Messages.getString(labelKey)), constraints);
        constraints.gridx = 1;
        fields.add(control, constraints);
    }

    /** The two reinforcement buttons, switched off until the player above them has been set up. */
    private JPanel reinforcePanel() {
        reinforceFromFileButton.addActionListener(event -> reinforce(clientGUI::reinforceFromFile));
        reinforceFromGeneratorButton.addActionListener(event -> reinforce(this::openUnitGeneratorFor));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(reinforceFromFileButton);
        panel.add(reinforceFromGeneratorButton);
        return panel;
    }

    /**
     * Hands the chosen player over to one of the reinforcement dialogs.
     *
     * @param reinforcement What to do with the player
     */
    private void reinforce(Consumer<Player> reinforcement) {
        Player player = chosenPlayer();
        if (player != null) {
            LOGGER.info("[GMPlayerSetup] reinforcing {}", player.getName());
            reinforcement.accept(player);
        }
    }

    /** Opens the unit generator already pointed at the given player. */
    private void openUnitGeneratorFor(Player player) {
        clientGUI.getRandomArmyDialog().setPlayerFrom(player);
        clientGUI.getRandomArmyDialog().setVisible(true);
    }

    private JPanel buttonPanel() {
        applyButton.addActionListener(event -> apply());
        JButton closeButton = new JButton(Messages.getString("Close"));
        closeButton.addActionListener(event -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(applyButton);
        panel.add(closeButton);
        return panel;
    }

    /**
     * Sets both choosers to what the chosen player already has.
     *
     * <p>This is the whole point of the dialog having its own form rather than a generated one: a gamemaster setting
     * a deployment zone must not have the team quietly changed to whatever a chooser happened to be showing.</p>
     */
    private void loadFromChosenPlayer() {
        Player player = chosenPlayer();
        if (player == null) {
            return;
        }
        if (player.getId() != playerReadyForUnits) {
            playerReadyForUnits = Player.PLAYER_NONE;
        }
        teamChooser.setSelectedItem(TeamChoice.of(player.getTeam()));
        if (teamChooser.getSelectedIndex() < 0) {
            teamChooser.setSelectedIndex(0);
        }
        int zone = player.getStartingPos();
        zoneChooser.setSelectedIndex(
              ((zone >= 0) && (zone < IStartingPositions.START_LOCATION_NAMES.length)) ? zone : 0);
        refreshLegality();
    }

    /**
     * Says whether the choices on screen can be applied, and turns Apply off while they cannot.
     *
     * <p>The server refuses some of these outright and the rest of them end a game, and neither is something to find
     * out after pressing a button.</p>
     */
    private void refreshLegality() {
        String problem = problemWithChoices();
        applyButton.setEnabled(problem == null);
        statusLabel.setText((problem == null)
              ? Messages.getString("GameMasterPlayerSetupDialog.ready")
              : Messages.getString("GameMasterPlayerSetupDialog.refused", problem));

        Player player = chosenPlayer();
        boolean readyForUnits = (player != null) && (player.getId() == playerReadyForUnits);
        reinforceFromFileButton.setEnabled(readyForUnits);
        reinforceFromGeneratorButton.setEnabled(readyForUnits);
        String tip = readyForUnits
              ? null
              : Messages.getString("GameMasterPlayerSetupDialog.reinforce.notYet");
        reinforceFromFileButton.setToolTipText(tip);
        reinforceFromGeneratorButton.setToolTipText(tip);
    }

    /**
     * @return why the chosen combination cannot be applied, or {@code null} when it can
     */
    private String problemWithChoices() {
        Player player = chosenPlayer();
        TeamChoice team = (TeamChoice) teamChooser.getSelectedItem();
        if ((player == null) || (team == null)) {
            return null;
        }
        int chosenTeam = resolveTeam(team);

        // the server refuses this one, so offering it only produces a message nobody asked for
        boolean holdsUnits = !clientGUI.getClient().getGame().getPlayerEntities(player, false).isEmpty();
        if ((chosenTeam == Player.TEAM_UNASSIGNED) && holdsUnits) {
            return Messages.getString("GameMasterPlayerSetupDialog.refused.unassignedWithUnits", player.getName());
        }
        if (wouldLeaveOneTeam(player, chosenTeam)) {
            return Messages.getString("GameMasterPlayerSetupDialog.refused.oneTeamLeft");
        }
        return null;
    }

    /**
     * Whether the change would leave everybody who is still playing on the same team.
     *
     * <p>A game with nobody left to fight is over, and the victory check runs immediately after a team change is
     * applied - so this would end the game rather than set somebody up in it.</p>
     *
     * @param player     The player being moved
     * @param chosenTeam The team they would end up on
     *
     * @return {@code true} when no opposing team would remain
     */
    private boolean wouldLeaveOneTeam(Player player, int chosenTeam) {
        if ((chosenTeam == Player.TEAM_UNASSIGNED) || (chosenTeam == Player.TEAM_NONE)) {
            // lone wolves are everybody's enemy, so this cannot empty the board of opposition
            return false;
        }
        for (Player other : clientGUI.getClient().getGame().getPlayersList()) {
            boolean isTheOneMoving = other.getId() == player.getId();
            boolean isPlaying = !other.isGhost() && (other.getTeam() != Player.TEAM_UNASSIGNED);
            if (!isTheOneMoving && isPlaying && (other.getTeam() != chosenTeam)) {
                return false;
            }
        }
        return true;
    }

    /** @return the names of the players already on the given team, in player order */
    private List<String> membersOf(int teamId) {
        return clientGUI.getClient()
              .getGame()
              .getPlayersList()
              .stream()
              .filter(player -> player.getTeam() == teamId)
              .map(Player::getName)
              .toList();
    }

    /**
     * @return the lowest numbered team that nobody is on, or {@link Player#TEAM_UNASSIGNED} when every team in the
     *       rules is already in use
     */
    private int firstUnusedTeam() {
        for (int teamId = Player.TEAM_NONE + 1; teamId < Player.TEAM_NAMES.length; teamId++) {
            final int teamBeingTested = teamId;
            boolean anybodyOnIt = clientGUI.getClient()
                  .getGame()
                  .getPlayersList()
                  .stream()
                  .anyMatch(player -> player.getTeam() == teamBeingTested);
            if (!anybodyOnIt) {
                return teamId;
            }
        }
        return Player.TEAM_UNASSIGNED;
    }

    /**
     * Whether a player is someone this dialog is for.
     *
     * <p>The gamemaster is left out: they set themselves up the way any player does, and this dialog exists for the
     * people they are bringing into the game. A player who has dropped out is left out as well, since setting up
     * somebody who is not connected achieves nothing.</p>
     *
     * @param player The player being considered
     *
     * @return {@code true} when the player may be set up here
     */
    private boolean mayBeSetUp(Player player) {
        boolean isTheGamemaster = player.getId() == clientGUI.getClient().getLocalPlayer().getId();
        return !isTheGamemaster && !player.isGhost();
    }

    /** @return the player the dialog is currently set to, or {@code null} when the game has none */
    private @Nullable Player chosenPlayer() {
        PlayerChoice choice = (PlayerChoice) playerChooser.getSelectedItem();
        return (choice == null) ? null : clientGUI.getClient().getGame().getPlayer(choice.playerId());
    }

    /**
     * Sends whichever of the two settings was actually changed.
     *
     * <p>Both go as gamemaster commands, which is what already carries them: the server checks the gamemaster role
     * and applies a team change without a vote. Sending nothing for an unchanged value keeps this from announcing
     * changes that were not made.</p>
     */
    private void apply() {
        Player player = chosenPlayer();
        if (player == null) {
            return;
        }
        TeamChoice team = (TeamChoice) teamChooser.getSelectedItem();
        ZoneChoice zone = (ZoneChoice) zoneChooser.getSelectedItem();
        int chosenTeamId = (team == null) ? player.getTeam() : resolveTeam(team);
        boolean teamChanged = chosenTeamId != player.getTeam();
        boolean zoneChanged = (zone != null) && (zone.zoneId() != player.getStartingPos());

        if (teamChanged) {
            LOGGER.info("[GMPlayerSetup] moving {} to team {}", player.getName(), chosenTeamId);
            clientGUI.getClient().sendChat("/changeTeam playerID=" + player.getId() + " teamID=" + chosenTeamId);
        }
        if (zoneChanged) {
            LOGGER.info("[GMPlayerSetup] setting the deployment zone of {} to {}", player.getName(), zone.zoneName());
            clientGUI.getClient()
                  .sendChat("/changeDeploymentZone playerID=" + player.getId() + " zoneID=" + zone.zoneId());
        }
        if (!teamChanged && !zoneChanged) {
            LOGGER.info("[GMPlayerSetup] nothing to change for {}", player.getName());
        }
        // the dialog stays open so the force can be handed over straight away, which is the rest of the same job
        playerReadyForUnits = player.getId();
        refreshLegality();
    }

    /**
     * @param team The team chosen in the chooser
     *
     * @return the team to send, turning the "new team" choice into a real team number
     */
    private int resolveTeam(TeamChoice team) {
        return (team.teamId() == TeamChoice.NEW_TEAM) ? firstUnusedTeam() : team.teamId();
    }

    /** @return the deployment zones offered, for tests */
    static List<String> zoneNames() {
        return List.of(IStartingPositions.START_LOCATION_NAMES);
    }
}
