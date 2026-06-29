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
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Player;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EnemyArtilleryInbound;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * A small, non-modal View-menu window listing every artillery round currently in the air that the local player is
 * entitled to see: who fired it, how many turns until it lands, the target hex, and the warhead type. The list is
 * sourced from {@link Game#getArtilleryAttacks()}, which the server has already filtered to the local player's team
 * (the sole exception being a GameMaster / observer, who sees all) - so the window respects double-blind without any
 * extra visibility logic here.
 *
 * @author HammerGS
 */
public class RoundsInAirDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final Client client;
    private final DefaultTableModel tableModel;

    public RoundsInAirDialog(JFrame parent, Client client) {
        super(parent, "", false);
        this.client = client;
        setTitle(Messages.getString("RoundsInAirDialog.title"));

        tableModel = new DefaultTableModel(columnNames(), 0) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Closing the window unticks the View-menu item so the menu state stays in sync.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                GUIP.setRoundsInAirEnabled(false);
            }
        });

        refresh();
        setMinimumSize(new Dimension(520, 220));
        pack();
        setLocation(GUIP.getRoundsInAirPosX(), GUIP.getRoundsInAirPosY());
    }

    private static String[] columnNames() {
        return new String[] {
              Messages.getString("RoundsInAirDialog.col.team"),
              Messages.getString("RoundsInAirDialog.col.player"),
              Messages.getString("RoundsInAirDialog.col.firedBy"),
              Messages.getString("RoundsInAirDialog.col.landsIn"),
              Messages.getString("RoundsInAirDialog.col.targetHex"),
              Messages.getString("RoundsInAirDialog.col.warhead")
        };
    }

    /**
     * Rebuilds the table from the current in-flight artillery, sorted so the soonest-landing rounds are at the top. Own
     * and allied rounds are shown in full; enemy rounds (a redacted, team-safe feed from the server) show only their
     * landing time, with the target hex and warhead listed as "Unknown". Safe to call repeatedly (e.g. on phase change).
     */
    public void refresh() {
        tableModel.setRowCount(0);
        Game game = client.getGame();
        List<RoundRow> rows = new ArrayList<>();

        // Own and allied rounds: full detail.
        for (Enumeration<ArtilleryAttackAction> attackEnumeration = game.getArtilleryAttacks();
              attackEnumeration.hasMoreElements(); ) {
            ArtilleryAttackAction attack = attackEnumeration.nextElement();
            rows.add(new RoundRow(attack.getTurnsTilHit(), new Object[] {
                  teamName(game, attack.getPlayerId()),
                  playerName(game, attack.getPlayerId()),
                  firingUnitName(game, attack.getEntityId()),
                  landsInText(attack.getTurnsTilHit()),
                  targetHexText(game, attack),
                  warheadName(attack)
            }));
        }

        // Enemy rounds: redacted - landing time only; the target hex and warhead are withheld (shown as "Unknown").
        String unknown = Messages.getString("RoundsInAirDialog.unknown");
        for (EnemyArtilleryInbound inbound : game.getEnemyArtilleryInbound()) {
            rows.add(new RoundRow(inbound.turnsTilHit(), new Object[] {
                  teamName(game, inbound.playerId()),
                  playerName(game, inbound.playerId()),
                  firingUnitName(game, inbound.firingEntityId()),
                  landsInText(inbound.turnsTilHit()),
                  unknown,
                  unknown
            }));
        }

        // Sort allied and enemy rounds together so the soonest-landing round is always at the top, regardless of side.
        rows.sort(Comparator.comparingInt(RoundRow::turnsTilHit));
        for (RoundRow row : rows) {
            tableModel.addRow(row.cells());
        }
    }

    /**
     * One row of the table, carrying its landing time separately so allied and enemy rounds can be sorted together by
     * soonest landing before being rendered.
     *
     * @param turnsTilHit Turns until this round lands (the sort key)
     * @param cells       The rendered cell values for the row, in column order
     */
    private record RoundRow(int turnsTilHit, Object[] cells) {}

    private static String teamName(Game game, int playerId) {
        Player player = game.getPlayer(playerId);
        if (player == null) {
            return Messages.getString("RoundsInAirDialog.unknownPlayer");
        }
        int team = player.getTeam();
        return ((team >= 0) && (team < Player.TEAM_NAMES.length)) ? Player.TEAM_NAMES[team] : String.valueOf(team);
    }

    private static String firingUnitName(Game game, int firingEntityId) {
        Entity firingEntity = game.getEntity(firingEntityId);
        return (firingEntity != null) ? firingEntity.getShortName()
              : Messages.getString("RoundsInAirDialog.unknownUnit");
    }

    private static String playerName(Game game, int playerId) {
        Player player = game.getPlayer(playerId);
        return (player != null) ? player.getName() : Messages.getString("RoundsInAirDialog.unknownPlayer");
    }

    private static String landsInText(int turns) {
        return (turns <= 0) ? Messages.getString("RoundsInAirDialog.landsThisTurn")
              : Messages.getString("RoundsInAirDialog.landsInTurns", turns);
    }

    private static String targetHexText(Game game, ArtilleryAttackAction attack) {
        Targetable target = attack.getTarget(game);
        // A counter-battery shot aims at an off-board enemy battery, whose virtual board number is meaningless as a
        // grid square, so label it instead of printing that number (or losing the value entirely).
        if ((target != null) && target.isOffBoard()) {
            return Messages.getString("RoundsInAirDialog.counterBattery");
        }
        if ((target != null) && (target.getPosition() != null)) {
            return target.getPosition().getBoardNum();
        }
        Coords coords = attack.getCoords();
        return (coords != null) ? coords.getBoardNum() : Messages.getString("RoundsInAirDialog.unknownHex");
    }

    /**
     * @param attack The in-flight artillery attack
     *
     * @return A short, human-readable warhead label derived from the recorded munition type (e.g. {@code Homing},
     *       {@code Cluster}), defaulting to {@code Standard (HE)} when no special munition is flagged
     */
    private static String warheadName(ArtilleryAttackAction attack) {
        var munitions = attack.getAmmoMunitionType();
        if (munitions.contains(Munitions.M_HOMING)) {
            return Messages.getString("RoundsInAirDialog.warhead.homing");
        } else if (munitions.contains(Munitions.M_CLUSTER)) {
            return Messages.getString("RoundsInAirDialog.warhead.cluster");
        } else if (munitions.contains(Munitions.M_FASCAM)) {
            return Messages.getString("RoundsInAirDialog.warhead.fascam");
        } else if (munitions.contains(Munitions.M_INFERNO_IV)) {
            return Messages.getString("RoundsInAirDialog.warhead.inferno");
        } else if (munitions.contains(Munitions.M_FLARE)) {
            return Messages.getString("RoundsInAirDialog.warhead.flare");
        } else if (munitions.contains(Munitions.M_SMOKE)) {
            return Messages.getString("RoundsInAirDialog.warhead.smoke");
        } else if (munitions.contains(Munitions.M_ADA)) {
            return Messages.getString("RoundsInAirDialog.warhead.ada");
        }
        return Messages.getString("RoundsInAirDialog.warhead.standard");
    }

    public void saveSettings() {
        GUIP.setRoundsInAirPosX(getLocation().x);
        GUIP.setRoundsInAirPosY(getLocation().y);
    }

    @Override
    protected void processWindowEvent(WindowEvent event) {
        super.processWindowEvent(event);
        if ((event.getID() == WindowEvent.WINDOW_DEACTIVATED) || (event.getID() == WindowEvent.WINDOW_CLOSING)) {
            saveSettings();
        }
    }
}
