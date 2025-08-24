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

import static megamek.client.ui.util.UIUtil.WARNING_SIGN;
import static megamek.client.ui.util.UIUtil.alternateTableBGColor;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.client.ui.util.UIUtil.uiGreen;
import static megamek.client.ui.util.UIUtil.uiYellow;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.MegaMek;
import megamek.client.bot.BotClient;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Board;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.Player;
import megamek.common.options.OptionsConstants;

class PlayerTable extends JTable {

    private static final int PLAYER_TABLE_ROW_HEIGHT = 45;

    PlayerTableModel model;
    ChatLounge lobby;

    public PlayerTable(PlayerTableModel pm, ChatLounge cl) {
        super(pm);
        model = pm;
        lobby = cl;
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        setDefaultRenderer(Player.class, new PlayerRenderer());
        TableColumn column = getColumnModel().getColumn(0);
        column.setHeaderValue(Messages.getString("ChatLounge.Players"));
        setRowHeights();
        pm.addTableModelListener(e -> setRowHeights());
    }

    void setRowHeights() {
        setRowHeight(UIUtil.scaleForGUI(PLAYER_TABLE_ROW_HEIGHT));
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        setRowHeights();
        super.columnMarginChanged(e);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        Player player = model.getPlayerAt(rowAtPoint(p));
        if (player == null) {
            return null;
        }

        StringBuilder result = new StringBuilder("<HTML><BODY>");
        result.append("<FONT").append(UIUtil.colorString(player.getColour().getColour())).append(">");
        result.append(player.getName());
        result.append("</FONT>");

        if ((lobby.client() instanceof BotClient) && player.equals(lobby.localPlayer())) {
            String msgThisBot = Messages.getString("ChatLounge.ThisBot");
            result.append(" (" + UIUtil.BOT_MARKER + " ").append(msgThisBot).append(")");
        } else if (lobby.client().getBots().containsKey(player.getName())) {
            String msgYourBot = Messages.getString("ChatLounge.YourBot");
            result.append(" (" + UIUtil.BOT_MARKER + " ").append(msgYourBot).append(")");
        } else if (lobby.localPlayer().equals(player)) {
            String msg_you = Messages.getString("ChatLounge.You");
            result.append(" (").append(msg_you).append(")");
        }
        result.append("<BR>");
        if (player.getConstantInitBonus() != 0) {
            String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
            String msgInitiativeModifier = Messages.getString("ChatLounge.InitiativeModifier");
            result.append(msgInitiativeModifier).append(": ").append(sign);
            result.append(player.getConstantInitBonus());
        } else {
            String msgNoInitiativeModifier = Messages.getString("ChatLounge.NoInitiativeModifier");
            result.append(msgNoInitiativeModifier);
        }
        if (lobby.game().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            int mines = player.getNbrMFConventional() + player.getNbrMFActive()
                  + player.getNbrMFInferno() + player.getNbrMFVibra();
            String msgTotalMinefields = Messages.getString("ChatLounge.TotalMinefields");
            result.append("<BR>").append(msgTotalMinefields).append(": ").append(mines);
        }
        return result.toString();
    }

    public static class PlayerTableModel extends AbstractTableModel {

        static final int COL_PLAYER = 0;
        static final int N_COL = 1;

        private final ArrayList<Player> players;

        public PlayerTableModel() {
            players = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return players.size();
        }

        void replaceData(List<Player> newPlayers) {
            players.clear();
            players.addAll(newPlayers);
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            return Messages.getString("ChatLounge.colPlayer");
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return Player.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return getPlayerAt(row);
        }

        Player getPlayerAt(int row) {
            return players.get(row);
        }
    }

    class PlayerRenderer extends DefaultTableCellRenderer {

        public PlayerRenderer() {
            setLayout(new GridLayout(1, 1, 5, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        }

        private void setImage(Image img) {
            setIcon(new ImageIcon(img));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {

            Player player = (Player) value;

            StringBuilder result = new StringBuilder("<HTML><NOBR>");
            // First Line - Player Name
            if ((lobby.client() instanceof BotClient) && player.equals(lobby.localPlayer())
                  || lobby.client().getBots().containsKey(player.getName())) {
                result.append(UIUtil.BOT_MARKER);
            }
            result.append(player.getName());
            result.append("<BR>");

            // Second Line - Team
            boolean isEnemy = lobby.localPlayer().isEnemyOf(player);
            Color color = isEnemy ? GUIPreferences.getInstance().getWarningColor() : uiGreen();
            result.append("<FONT").append(UIUtil.colorString(color)).append(">");
            result.append(Player.TEAM_NAMES[player.getTeam()]);
            result.append("</FONT>");

            // Deployment Position
            result.append(UIUtil.DOT_SPACER);

            String msg_start = Messages.getString("ChatLounge.Start");

            final var gOpts = lobby.game().getOptions();
            if (gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)
                  && !player.isBot()
                  && player.getId() != 0) {
                result.append(msg_start).append(": ").append(Messages.getString("ChatLounge.Player0"));
            } else if ((!lobby.client().getLocalPlayer().isGameMaster()
                  && (isEnemy)
                  && (gOpts.booleanOption(OptionsConstants.BASE_BLIND_DROP)
                  || gOpts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)))) {
                result.append(msg_start).append(": ").append(Messages.getString("ChatLounge.Blind"));
            } else if ((player.getStartingPos() >= 0)
                  && (player.getStartingPos() <= IStartingPositions.START_LOCATION_NAMES.length)) {
                result.append(msg_start)
                      .append(": ")
                      .append(IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);

                if (player.getStartingPos() == 0) {
                    int NWx = player.getStartingAnyNWx() + 1;
                    int NWy = player.getStartingAnyNWy() + 1;
                    int SEx = player.getStartingAnySEx() + 1;
                    int SEy = player.getStartingAnySEy() + 1;
                    if ((NWx + NWy + SEx + SEy) > 0) {
                        result.append(" (")
                              .append(NWx)
                              .append(", ")
                              .append(NWy)
                              .append(")-(")
                              .append(SEx)
                              .append(", ")
                              .append(SEy)
                              .append(")");
                    }
                }
                int so = player.getStartOffset();
                int sw = player.getStartWidth();
                if ((so != 0) || (sw != 3)) {
                    result.append(", ").append(so);
                    result.append(", ").append(sw);
                }
            } else if (player.getStartingPos() > IStartingPositions.START_LOCATION_NAMES.length) {
                result.append(msg_start)
                      .append(": ")
                      .append("Zone ")
                      .append(Board.decodeCustomDeploymentZoneID(player.getStartingPos()));
            }

            if (!LobbyUtility.isValidStartPos(lobby.game(), player)) {
                result.append("<FONT").append(UIUtil.colorString(uiYellow())).append(">");
                result.append(WARNING_SIGN);
                result.append("</FONT>");
            }

            // Player BV
            result.append(UIUtil.DOT_SPACER);
            String msgBVPlain = Messages.getString("ChatLounge.BVplain");
            result.append(msgBVPlain).append(": ");
            NumberFormat formatter = NumberFormat.getIntegerInstance(MegaMek.getMMOptions().getLocale());
            result.append((player.getBV() != 0) ? formatter.format(player.getBV()) : "--");

            // Initiative Mod
            if (player.getConstantInitBonus() != 0) {
                result.append(UIUtil.DOT_SPACER);
                String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
                String msg_init = Messages.getString("ChatLounge.Init");
                result.append(msg_init).append(": ").append(sign);
                result.append(player.getConstantInitBonus());
            }

            if (player.getSingleBlind()) {
                result.append(UIUtil.DOT_SPACER);
                result.append("<FONT").append(UIUtil.colorString(uiGreen())).append(">");
                result.append("\uD83D\uDC41");
                result.append("</FONT>");
            }

            if (player.getGameMaster()) {
                result.append(UIUtil.DOT_SPACER);
                result.append("<FONT").append(UIUtil.colorString(uiGreen())).append(">");
                result.append("\uD83D\uDD2E  GM");
                result.append("</FONT>");
            }

            setText(result.toString());

            setIconTextGap(10);
            Image camo = player.getCamouflage().getImage();
            int size = scaleForGUI(PLAYER_TABLE_ROW_HEIGHT) / 2;
            setImage(camo.getScaledInstance(-1, size, Image.SCALE_SMOOTH));

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                Color background = table.getBackground();
                if (row % 2 != 0) {
                    background = alternateTableBGColor();
                }
                setBackground(background);
            }

            if (hasFocus) {
                if (!isSelected) {
                    Color col = UIManager.getColor("Table.focusCellForeground");
                    if (col != null) {
                        setForeground(col);
                    }
                    col = UIManager.getColor("Table.focusCellBackground");
                    if (col != null) {
                        setBackground(col);
                    }
                }
            }
            return this;
        }
    }
}
