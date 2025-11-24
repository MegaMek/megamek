/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.FONT_SCALE1;
import static megamek.client.ui.util.UIUtil.criticalSign;
import static megamek.client.ui.util.UIUtil.fontHTML;
import static megamek.client.ui.util.UIUtil.warningSign;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.TableColumnManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;

/**
 * A JPanel that holds a table giving an overview of the current relative strength of the teams of the game. The table
 * does not listen to game changes and requires being notified through {@link #refreshData()}. It accesses data through
 * the stored ClientGUI.
 */
public class TeamOverviewPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = -4754010220963493049L;

    private enum TOP_COLS {
        TEAM, MEMBERS, TONNAGE, COST, BV, HIDDEN, UNITS
    }

    private final TeamOverviewModel teamOverviewModel = new TeamOverviewModel();
    private final JTable teamOverviewTable = new JTable(teamOverviewModel);
    private final TableColumnManager teamOverviewManager = new TableColumnManager(teamOverviewTable, false);
    private final ClientGUI clientGui;
    private boolean isDetached;
    private int shownColumn;

    /**
     * Constructs the team overview panel; the given ClientGUI is used to access the game data.
     */
    public TeamOverviewPanel(ClientGUI cg) {
        clientGui = cg;
        setLayout(new GridLayout(1, 1));
        teamOverviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamOverviewTable.getSelectionModel().addListSelectionListener(e -> repaint());
        teamOverviewTable.getTableHeader().setReorderingAllowed(false);
        teamOverviewTable.getTableHeader().addMouseListener(headerListener);
        var colModel = teamOverviewTable.getColumnModel();
        colModel.getColumn(TOP_COLS.MEMBERS.ordinal()).setCellRenderer(new MemberListRenderer());
        var centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        colModel.getColumn(TOP_COLS.TONNAGE.ordinal()).setCellRenderer(centerRenderer);
        colModel.getColumn(TOP_COLS.COST.ordinal()).setCellRenderer(centerRenderer);
        colModel.getColumn(TOP_COLS.BV.ordinal()).setCellRenderer(centerRenderer);
        colModel.getColumn(TOP_COLS.TEAM.ordinal()).setCellRenderer(centerRenderer);
        colModel.getColumn(TOP_COLS.HIDDEN.ordinal()).setCellRenderer(centerRenderer);
        JScrollPane scrTeams = new JScrollPane(teamOverviewTable);
        scrTeams.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrTeams);

        refreshData();
    }

    /** Detaches or attaches the team overview from/to its pane. */
    public void setDetached(boolean state) {
        if (state != isDetached) {
            isDetached = state;
            if (isDetached) {
                shownColumn = 0;
                teamOverviewManager.hideColumn(TOP_COLS.TONNAGE.ordinal());
                teamOverviewManager.hideColumn(TOP_COLS.COST.ordinal());
            } else {
                teamOverviewManager.hideColumn(TOP_COLS.TONNAGE.ordinal());
                teamOverviewManager.hideColumn(TOP_COLS.COST.ordinal());
                teamOverviewManager.hideColumn(TOP_COLS.BV.ordinal());
                teamOverviewManager.showColumn(TOP_COLS.TONNAGE.ordinal());
                teamOverviewManager.showColumn(TOP_COLS.COST.ordinal());
                teamOverviewManager.showColumn(TOP_COLS.BV.ordinal());
            }
            refreshTableHeader();
        }
    }

    MouseListener headerListener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (isDetached) {
                shownColumn = (shownColumn + 1) % 3;
                if (shownColumn == 0) {
                    teamOverviewManager.hideColumn(TOP_COLS.TONNAGE.ordinal());
                    teamOverviewManager.hideColumn(TOP_COLS.COST.ordinal());
                    teamOverviewManager.showColumn(TOP_COLS.BV.ordinal());
                } else if (shownColumn == 1) {
                    teamOverviewManager.hideColumn(TOP_COLS.TONNAGE.ordinal());
                    teamOverviewManager.showColumn(TOP_COLS.COST.ordinal());
                    teamOverviewManager.hideColumn(TOP_COLS.BV.ordinal());
                } else {
                    teamOverviewManager.showColumn(TOP_COLS.TONNAGE.ordinal());
                    teamOverviewManager.hideColumn(TOP_COLS.COST.ordinal());
                    teamOverviewManager.hideColumn(TOP_COLS.BV.ordinal());
                }
            }
        }
    };

    /** Refreshes the headers, setting the header names and gui scaling them. */
    public void refreshTableHeader() {
        JTableHeader header = teamOverviewTable.getTableHeader();
        for (int i = 0; i < teamOverviewTable.getColumnCount(); i++) {
            TableColumn column = teamOverviewTable.getColumnModel().getColumn(i);
            column.setHeaderValue(teamOverviewModel.getColumnName(i));
        }
        header.repaint();
    }

    /** Updates the table with data from the game. */
    public void refreshData() {
        // Remember the previously selected team, if any
        int selectedRow = teamOverviewTable.getSelectedRow();
        int selectedTeam = -1;
        if (selectedRow != -1) {
            selectedTeam = teamOverviewModel.teamID.get(teamOverviewTable.getSelectedRow());
        }

        // Update the data
        teamOverviewModel.updateTable(clientGui.getClient().getGame());

        // Re-select the previously selected team, if possible
        if ((selectedRow != -1) && (teamOverviewModel.teamID.contains(selectedTeam))) {
            int row = teamOverviewModel.teamID.indexOf(selectedTeam);
            teamOverviewTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    /** The table model for the Team overview panel */
    private class TeamOverviewModel extends AbstractTableModel {
        @Serial
        private static final long serialVersionUID = 2747614890129092912L;

        private final ArrayList<Team> teams = new ArrayList<>();
        private final ArrayList<Integer> teamID = new ArrayList<>();
        private final ArrayList<String> teamNames = new ArrayList<>();
        private final ArrayList<Long> bvs = new ArrayList<>();
        private final ArrayList<Long> costs = new ArrayList<>();
        private final ArrayList<Long> tons = new ArrayList<>();
        private final ArrayList<String> units = new ArrayList<>();
        private final ArrayList<Double> hidden = new ArrayList<>();

        @Override
        public int getRowCount() {
            return teams.size();
        }

        public void clearData() {
            teams.clear();
            teamID.clear();
            teamNames.clear();
            bvs.clear();
            costs.clear();
            tons.clear();
            units.clear();
            hidden.clear();
        }

        @Override
        public int getColumnCount() {
            return TOP_COLS.values().length;
        }

        /** Updates the stored data from the provided game. */
        public void updateTable(Game game) {
            clearData();
            for (Team team : game.getTeams()) {
                teams.add(team);
                teamID.add(team.getId());
                teamNames.add(team.toString());

                long cost = 0;
                double ton = 0;
                int bv = 0;
                int[] unitCounts = { 0, 0, 0, 0, 0 };
                int hiddenBv = 0;
                boolean[] unitCritical = { false, false, false, false, false };
                boolean[] unitWarnings = { false, false, false, false, false };
                for (Player teamMember : team.players()) {
                    // Get the "real" player object, as the team's may be wrong
                    Player player = game.getPlayer(teamMember.getId());
                    bv += player.getBV();
                    for (Entity entity : game.getPlayerEntities(player, false)) {
                        // Avoid counting fighters in squadrons twice
                        if (entity instanceof FighterSquadron) {
                            continue;
                        }
                        cost += (long) entity.getCost(false);
                        ton += entity.getWeight();
                        unitCounts[classIndex(entity)]++;
                        int mapType = clientGui.getClient().getMapSettings().getMedium();
                        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                              || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                              || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                              || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                              || (!entity.isDesignValid())) {
                            unitCritical[classIndex(entity)] = true;
                        }
                        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes()
                              == (Entity.MAX_C3i_NODES - 1)))
                              || (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == (Entity.MAX_NOVA_CEWS_NODES
                              - 1)))
                              || ((entity.getC3Master() == null) && entity.hasC3S())) {
                            unitWarnings[classIndex(entity)] = true;
                        }
                        if (entity.isHidden()) {
                            hiddenBv += entity.calculateBattleValue();
                        }
                    }
                }
                units.add(unitSummary(unitCounts, unitCritical, unitWarnings));
                bvs.add((long) bv);
                hidden.add(bv != 0 ? (double) hiddenBv / bv : 0);
                costs.add(cost);
                tons.add((long) (ton * 1000));
            }
            teamOverviewTable.clearSelection();
            fireTableDataChanged();
            updateRowHeights();
        }

        private int classIndex(Entity entity) {
            if (entity instanceof Mek) {
                return 0;
            } else if (entity instanceof Tank) {
                return 1;
            } else if (entity instanceof Aero) {
                return 2;
            } else if (entity instanceof Infantry) {
                return 3;
            } else { // ProtoMek
                return 4;
            }
        }

        private String unitSummary(int[] counts, boolean[] criticalSlots, boolean[] warnings) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) {
                    result.append(criticalSlots[i] ? criticalSign() + " " : "");
                    result.append(warnings[i] ? warningSign() + " " : "");
                    result.append(Messages.getString("ChatLounge.teamOverview.unitSum" + i))
                          .append(" ")
                          .append(counts[i]);
                    result.append("<BR>");
                }

            }
            return result.toString();
        }

        /**
         * Finds and sets the required row height (max height of all cells plus margin).
         */
        private void updateRowHeights() {
            int rowHeight = 0;
            for (int row = 0; row < teamOverviewTable.getRowCount(); row++) {
                for (int col = 0; col < teamOverviewTable.getColumnCount(); col++) {
                    // Consider the preferred height of the team members column
                    TableCellRenderer renderer = teamOverviewTable.getCellRenderer(row, col);
                    Component comp = teamOverviewTable.prepareRenderer(renderer, row, col);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }
            }
            // Add a little margin to the rows
            teamOverviewTable.setRowHeight(rowHeight + 20);
        }

        @Override
        public String getColumnName(int column) {
            column += (isDetached && column > 1) ? 2 : 0;
            String text = Messages.getString("ChatLounge.teamOverview.COL" + TOP_COLS.values()[column]);
            float textSizeDelta = isDetached ? 0f : 0.3f;
            return "<HTML><NOBR>" + UIUtil.fontHTML(textSizeDelta) + text;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public Object getValueAt(int row, int col) {
            StringBuilder result = new StringBuilder("<HTML><NOBR>");
            TOP_COLS column = TOP_COLS.values()[col];
            switch (column) {
                case TEAM:
                    boolean isEnemy = !teams.get(row).players().contains(clientGui.getClient().getLocalPlayer());
                    Color color = isEnemy ? GUIPreferences.getInstance().getEnemyUnitColor()
                          : GUIPreferences.getInstance().getMyUnitColor();
                    result.append(UIUtil.fontHTML(color)).append("&nbsp;");
                    result.append(teamNames.get(row)).append("</FONT>");
                    break;

                case TONNAGE:
                    result.append(fontHTML()).append("<CENTER>");
                    double ton = (double) tons.get(row) / 1000;
                    if (ton < 10) {
                        result.append(String.format("%.2f", ton)).append(" Tons");
                    } else {
                        result.append(String.format("%,d", Math.round(ton))).append(" Tons");
                    }
                    result.append(relativeValue(tons, row));
                    break;

                case COST:
                    result.append(fontHTML()).append("<CENTER>");
                    if (costs.get(row) < 10_000_000) {
                        result.append(String.format("%,d", costs.get(row))).append(" C-Bills");
                    } else {
                        result.append(String.format("%,d", costs.get(row) / 1_000_000)).append("\u00B7M C-Bills");
                    }
                    result.append(relativeValue(costs, row));
                    break;

                case MEMBERS:
                    return teams.get(row).players();

                case BV:
                    result.append(fontHTML()).append("<CENTER>");
                    result.append(NumberFormat.getIntegerInstance().format(bvs.get(row)));
                    result.append(relativeValue(bvs, row));
                    break;

                case UNITS:
                    if (!seeTeam(row)) {
                        return "<HTML>" + UIUtil.fontHTML(UIUtil.uiGray()) + "Unavailable";
                    }
                    result.append(fontHTML());
                    result.append(units.get(row));
                    break;

                case HIDDEN:
                    result.append(fontHTML()).append("<CENTER>");
                    var percentage = hidden.get(row);
                    result.append(percentage == 0 ? "--" : NumberFormat.getPercentInstance().format(percentage));

                default:
                    break;
            }

            return result.toString();
        }

        private boolean seeTeam(int row) {
            Game game = clientGui.getClient().getGame();
            return !game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)
                  || game.getTeamForPlayer(clientGui.getClient().getLocalPlayer()).getId() == teamID.get(row);
        }

        /**
         * Constructs and returns the string "(xx % of Team yy)". The provided values list is the data for the table
         * column and the provided row is the row of current value. The reference value (that represents 100%) is taken
         * from the selected row. Returns an empty string if nothing is selected or the base value is 0.
         */
        private String relativeValue(ArrayList<Long> values, int row) {
            int selectedRow = teamOverviewTable.getSelectedRow();
            boolean hasSelection = selectedRow != -1;
            if (hasSelection && (selectedRow != row)) {
                long baseValue = values.get(selectedRow);
                if (baseValue != 0) {
                    String selectedTeam = teamNames.get(selectedRow);
                    long percentage = 100 * values.get(row) / baseValue;
                    if (isDetached) {
                        return "<BR>" + UIUtil.fontHTML(UIUtil.uiGray())
                              + String.format("(%d %%)", percentage);
                    } else {
                        return "<BR>" + UIUtil.fontHTML(UIUtil.uiGray())
                              + String.format("(%d %% of %s)", percentage, selectedTeam);
                    }
                }
            }
            return "";
        }

    }

    /** A specialized renderer for the mek table. */
    private class MemberListRenderer extends JPanel implements TableCellRenderer {
        @Serial
        private static final long serialVersionUID = 6379065972840999336L;

        MemberListRenderer() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {

            if (!(value instanceof List<?> playerList)) {
                return null;
            }
            removeAll();
            add(Box.createVerticalGlue());
            int baseSize = FONT_SCALE1 - (isDetached ? 2 : 0);
            int size = 2 * baseSize;
            Font font = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, baseSize);
            for (Object obj : playerList) {
                if (!(obj instanceof Player player)) {
                    continue;
                }
                JLabel lblPlayer = new JLabel(player.getName());
                lblPlayer.setBorder(new EmptyBorder(3, 3, 3, 3));
                lblPlayer.setFont(font);
                lblPlayer.setIconTextGap(5);
                Image camo = player.getCamouflage().getImage();
                lblPlayer.setIcon(new ImageIcon(camo.getScaledInstance(-1, size, Image.SCALE_SMOOTH)));
                add(lblPlayer);
            }
            add(Box.createVerticalGlue());

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            return this;
        }
    }

}
