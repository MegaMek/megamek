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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.panels.phaseDisplay.lobby.sorters.FormationGrouping;
import megamek.client.ui.panels.phaseDisplay.lobby.sorters.FormationSorter;
import megamek.client.ui.panels.phaseDisplay.lobby.sorters.MekTableSorter;
import megamek.client.ui.panels.phaseDisplay.lobby.sorters.PlayerTransportIDSorter;
import megamek.client.ui.util.UIUtil;
import megamek.common.force.Forces;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Formations in the lobby's Sortable View: the "Group by Formation" and "Show Formations" toggles, the table sort that
 * keeps each formation together, the Formation column's visibility, and the C3 networks that grouping splits.
 * <p>
 * Kept out of {@link ChatLounge}, which only creates it, adds its buttons, hands it the table and delegates to it.
 * </p>
 */
class LobbyFormationDisplay {
    private static final MMLogger LOGGER = MMLogger.create(LobbyFormationDisplay.class);
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** Narrowest the Formation column may be dragged while shown, at GUI scale 1. */
    private static final int FORMATION_COLUMN_MIN_WIDTH = 30;

    private final MMToggleButton groupByFormationButton =
          new MMToggleButton(Messages.getString("ChatLounge.butGroupByFormation"));
    private final MMToggleButton showFormationsButton =
          new MMToggleButton(Messages.getString("ChatLounge.butShowFormations"));
    private final Runnable refreshTable;

    private JTable mekTable;
    /** C3 networks the table shows split across formations; their members get a tag, not branches. */
    private Set<String> splitC3NetIds = new HashSet<>();
    /** The table description last written to the log, so that only a change is logged. */
    private String lastLoggedDescription = "";

    /**
     * The buttons only change the preferences; the lobby passes the resulting change on to
     * {@link #preferenceChanged(String)}, which does the work, so a change made anywhere else takes the same path.
     *
     * @param refreshTable rebuilds the lobby's unit table; called when grouping is switched on or off
     */
    LobbyFormationDisplay(Runnable refreshTable) {
        this.refreshTable = refreshTable;
        groupByFormationButton.setToolTipText(Messages.getString("ChatLounge.butGroupByFormation.tooltip"));
        showFormationsButton.setToolTipText(Messages.getString("ChatLounge.butShowFormations.tooltip"));
        syncButtons();
        groupByFormationButton.addActionListener(event -> PreferenceManager.getClientPreferences()
              .setLobbyGroupByFormation(groupByFormationButton.isSelected()));
        showFormationsButton.addActionListener(event -> PreferenceManager.getClientPreferences()
              .setLobbyShowFormationColumn(showFormationsButton.isSelected()));
    }

    /** Adds the two toggle buttons to the lobby's button bar. */
    void addButtonsTo(JPanel panel) {
        panel.add(groupByFormationButton);
        panel.add(showFormationsButton);
    }

    /**
     * Hands over the lobby's unit table, once it exists: shows or hides its Formation column, and gives the column's
     * header a tooltip that explains the current formation order and what the next click does.
     *
     * @param table            the lobby's unit table
     * @param activeSorter     supplies the table's current sorter
     * @param formationSorters the Formation column's sorters, in the order a header click cycles through them
     */
    void attachTable(JTable table, Supplier<MekTableSorter> activeSorter, List<MekTableSorter> formationSorters) {
        mekTable = table;
        applyColumnVisibility();
        JTableHeader header = table.getTableHeader();
        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                boolean overFormation = header.columnAtPoint(event.getPoint()) == MekTableModel.COL_FORMATION;
                header.setToolTipText(overFormation ? headerTooltip(activeSorter.get(), formationSorters) : null);
            }
        });
    }

    /**
     * Returns the Formation column header's tooltip: what the current formation order puts first and what a click
     * changes it to, or, when the table is sorted some other way, which orders a click offers.
     */
    private static String headerTooltip(MekTableSorter activeSorter, List<MekTableSorter> formationSorters) {
        if (!(activeSorter instanceof FormationSorter current)) {
            return Messages.getString("ChatLounge.formationHeader.notSorted");
        }
        String next = current.description();
        int index = formationSorters.indexOf(current);
        for (int step = 1; step <= formationSorters.size(); step++) {
            MekTableSorter candidate = formationSorters.get((index + step) % formationSorters.size());
            if ((candidate instanceof FormationSorter nextSorter) && nextSorter.isAllowedNow()) {
                next = nextSorter.description();
                break;
            }
        }
        return "<html>" + current.description() + "<br>" + Messages.getString("ChatLounge.formationHeader.unitsInside")
              + "<br>" + Messages.getString("ChatLounge.formationHeader.next", next) + "</html>";
    }

    /**
     * Sorts the units for the table. Whatever the sorter, a train stays together and a C3 network stays together in
     * hierarchy order. With "Group by Formation" on, each formation is also one block of rows (lances inside
     * companies inside battalions), the C3 rule applying inside a formation, and the default sorter follows the force
     * tree order. Off, the whole force is one list.
     *
     * @param entities     the units to sort, sorted in place
     * @param forces       the game's forces
     * @param activeSorter the sorter the player chose
     */
    void sortForTable(List<Entity> entities, Forces forces, MekTableSorter activeSorter) {
        String blocks;
        // A Formation column sort orders whole formations, so it always groups
        boolean formationSort = activeSorter instanceof FormationSorter;
        if (formationSort || PreferenceManager.getClientPreferences().getLobbyGroupByFormation()) {
            boolean followTreeOrder = activeSorter instanceof PlayerTransportIDSorter;
            FormationGrouping grouping = new FormationGrouping(entities, forces, activeSorter, followTreeOrder);
            entities.sort(grouping.comparator());
            splitC3NetIds = grouping.splitC3NetIds();
            blocks = grouping.blocksDescription(entities);
        } else {
            entities.sort(MekTableSorter.keepingC3NetworksTogether(entities,
                  MekTableSorter.keepingCarriedUnitsTogether(activeSorter)));
            splitC3NetIds = new HashSet<>();
            blocks = "whole force, not grouped by formation";
        }
        logIfChanged(activeSorter.getDisplayName() + ": " + blocks
              + (splitC3NetIds.isEmpty() ? "" : "; C3 networks split across formations " + splitC3NetIds));
    }

    /**
     * Returns {@code true} when the table shows the unit's C3 network split across formations, so its rows cannot be
     * drawn as one network tree.
     */
    boolean isSplitC3Network(Entity entity) {
        String netId = entity.getC3NetId();
        return (netId != null) && splitC3NetIds.contains(netId);
    }

    /**
     * Follows a lobby preference change made elsewhere.
     *
     * @param preferenceName the name of the changed preference
     *
     * @return {@code true} if the preference was one of this display's
     */
    boolean preferenceChanged(String preferenceName) {
        switch (preferenceName) {
            case ClientPreferences.LOBBY_GROUP_BY_FORMATION -> {
                syncButtons();
                refreshTable.run();
                return true;
            }
            case ClientPreferences.LOBBY_SHOW_FORMATION_COLUMN -> {
                syncButtons();
                applyColumnVisibility();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Sets both buttons to the lobby preferences. Selecting a toggle in code does not fire its action. */
    private void syncButtons() {
        ClientPreferences preferences = PreferenceManager.getClientPreferences();
        groupByFormationButton.setSelected(preferences.getLobbyGroupByFormation());
        showFormationsButton.setSelected(preferences.getLobbyShowFormationColumn());
    }

    /**
     * Shows or hides the Formation column as the lobby preference says. A hidden column keeps its place at zero width,
     * so view and model column indices stay the same.
     */
    private void applyColumnVisibility() {
        if (mekTable == null) {
            return;
        }
        TableColumn column = mekTable.getColumnModel().getColumn(MekTableModel.COL_FORMATION);
        if (PreferenceManager.getClientPreferences().getLobbyShowFormationColumn()) {
            column.setMinWidth(UIUtil.scaleForGUI(FORMATION_COLUMN_MIN_WIDTH));
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setPreferredWidth(GUIP.getInt(GUIPreferences.LOBBY_MEK_TABLE_FORMATION_WIDTH));
        } else {
            column.setMinWidth(0);
            column.setMaxWidth(0);
            column.setPreferredWidth(0);
        }
    }

    private void logIfChanged(String description) {
        if (!description.equals(lastLoggedDescription)) {
            LOGGER.info("[FormationGroup] Sortable View by {}", description);
            lastLoggedDescription = description;
        }
    }
}
