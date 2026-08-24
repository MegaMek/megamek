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
import java.util.List;
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
import megamek.common.Team;
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

    /** One player offered in the chooser, named rather than numbered. */
    private record PlayerChoice(int playerId, String playerName) {
        @Override
        public String toString() {
            return playerName;
        }
    }

    /** One team offered in the chooser. */
    private record TeamChoice(int teamId) {
        @Override
        public String toString() {
            return (teamId == Player.TEAM_UNASSIGNED)
                  ? Messages.getString("GameMasterPlayerSetupDialog.noTeam")
                  : Messages.getString("GameMasterPlayerSetupDialog.team", teamId);
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
        for (Player player : clientGUI.getClient().getGame().getPlayersList()) {
            playerChooser.addItem(new PlayerChoice(player.getId(), player.getName()));
        }
        // the teams that exist, plus the one a player sits on before they are given any: a gamemaster taking someone
        // back out of the game needs the same list as one putting them in
        teamChooser.addItem(new TeamChoice(Player.TEAM_UNASSIGNED));
        for (Team team : clientGUI.getClient().getGame().getTeams()) {
            teamChooser.addItem(new TeamChoice(team.getId()));
        }
        for (int zone = 0; zone < IStartingPositions.START_LOCATION_NAMES.length; zone++) {
            zoneChooser.addItem(new ZoneChoice(zone, IStartingPositions.START_LOCATION_NAMES[zone]));
        }
        playerChooser.addActionListener(event -> loadFromChosenPlayer());

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
        constraints.gridwidth = 1;

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fields, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

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

    private JPanel buttonPanel() {
        JButton applyButton = new JButton(Messages.getString("GameMasterPlayerSetupDialog.apply"));
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
        teamChooser.setSelectedItem(new TeamChoice(player.getTeam()));
        if (teamChooser.getSelectedIndex() < 0) {
            teamChooser.setSelectedIndex(0);
        }
        int zone = player.getStartingPos();
        zoneChooser.setSelectedIndex(
              ((zone >= 0) && (zone < IStartingPositions.START_LOCATION_NAMES.length)) ? zone : 0);
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
        boolean teamChanged = (team != null) && (team.teamId() != player.getTeam());
        boolean zoneChanged = (zone != null) && (zone.zoneId() != player.getStartingPos());

        if (teamChanged) {
            LOGGER.info("[GMPlayerSetup] moving {} to team {}", player.getName(), team.teamId());
            clientGUI.getClient().sendChat("/changeTeam playerID=" + player.getId() + " teamID=" + team.teamId());
        }
        if (zoneChanged) {
            LOGGER.info("[GMPlayerSetup] setting the deployment zone of {} to {}", player.getName(), zone.zoneName());
            clientGUI.getClient()
                  .sendChat("/changeDeploymentZone playerID=" + player.getId() + " zoneID=" + zone.zoneId());
        }
        if (!teamChanged && !zoneChanged) {
            LOGGER.info("[GMPlayerSetup] nothing to change for {}", player.getName());
        }
        // closed either way: leaving it open after a change has been sent reads as nothing having happened
        dispose();
    }

    /** @return the deployment zones offered, for tests */
    static List<String> zoneNames() {
        return List.of(IStartingPositions.START_LOCATION_NAMES);
    }
}
