/*
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
package megamek.client.ui.swing.lobby;

import megamek.MegaMek;
import megamek.client.bot.BotClient;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IStartingPositions;
import megamek.common.Player;
import megamek.common.options.OptionsConstants;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static megamek.client.ui.swing.util.UIUtil.*;

class PlayerTable extends JTable {
    private static final long serialVersionUID = 6252953920509362407L;

    private static final int PLAYERTABLE_ROWHEIGHT = 45;

    PlayerTableModel model = new PlayerTableModel();
    ChatLounge lobby;

    public PlayerTable(PlayerTableModel pm, ChatLounge cl) {
        super(pm);
        model = pm;
        lobby = cl;
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        setDefaultRenderer(Player.class, new PlayerRenderer());
        TableColumn column = getColumnModel().getColumn(0);
        String msg_players = Messages.getString("ChatLounge.Players");
        column.setHeaderValue(msg_players);
    }

    void rescale() {
        setRowHeight(UIUtil.scaleForGUI(PLAYERTABLE_ROWHEIGHT));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        Player player = model.getPlayerAt(rowAtPoint(p));
        if (player == null) {
            return null;
        }

        StringBuilder result = new StringBuilder("<HTML>");
        result.append(guiScaledFontHTML(player.getColour().getColour()));
        result.append(player.getName() + "</FONT>");

        result.append(guiScaledFontHTML());
        if ((lobby.client() instanceof BotClient) && player.equals(lobby.localPlayer())) {
            String msg_thisbot = Messages.getString("ChatLounge.ThisBot");
            result.append(" (" + UIUtil.BOT_MARKER + " " + msg_thisbot + ")");
        } else if (lobby.client().bots.containsKey(player.getName())) {
            String msg_yourbot = Messages.getString("ChatLounge.YourBot");
            result.append(" (" + UIUtil.BOT_MARKER + " " + msg_yourbot + ")");
        } else if (lobby.localPlayer().equals(player)) {
            String msg_you = Messages.getString("ChatLounge.You");
            result.append(" (" + msg_you + ")");
        }
        result.append("<BR>");
        if (player.getConstantInitBonus() != 0) {
            String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
            String msg_initiativemodifier = Messages.getString("ChatLounge.InitiativeModifier");
            result.append(msg_initiativemodifier + ": ").append(sign);
            result.append(player.getConstantInitBonus());
        } else {
            String msg_noinitiativemodifier = Messages.getString("ChatLounge.NoInitiativeModifier");
            result.append(msg_noinitiativemodifier);
        }
        if (lobby.game().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            int mines = player.getNbrMFConventional() + player.getNbrMFActive() 
            + player.getNbrMFInferno() + player.getNbrMFVibra();
            String msg_totalminefields = Messages.getString("ChatLounge.TotalMinefields");
            result.append("<BR>" + msg_totalminefields + ": ").append(mines);
        }
        return result.toString();
    }

    public static class PlayerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1372393680232901923L;

        static final int COL_PLAYER = 0;
        static final int N_COL = 1;

        private ArrayList<Player> players;

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
            return "<HTML>" + UIUtil.guiScaledFontHTML() + Messages.getString("ChatLounge.colPlayer");
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

    class PlayerRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
        private static final long serialVersionUID = 4947299735765324311L;

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

            StringBuilder result = new StringBuilder("<HTML><NOBR>" + UIUtil.guiScaledFontHTML());
            // First Line - Player Name
            if ((lobby.client() instanceof BotClient) && player.equals(lobby.localPlayer())
                    || lobby.client().bots.containsKey(player.getName())) {
                result.append(UIUtil.BOT_MARKER);
            }
            result.append(player.getName());
            result.append("<BR>");

            // Second Line - Team
            boolean isEnemy = lobby.localPlayer().isEnemyOf(player);
            Color color = isEnemy ? GUIPreferences.getInstance().getWarningColor() : uiGreen();
            result.append(guiScaledFontHTML(color));
            result.append(Player.TEAM_NAMES[player.getTeam()]);
            result.append("</FONT>");

            // Deployment Position
            result.append(UIUtil.DOT_SPACER);
            result.append(guiScaledFontHTML());

            String msg_start = Messages.getString("ChatLounge.Start");
            if ((player.getStartingPos() >= 0) && (player.getStartingPos() <= IStartingPositions.START_LOCATION_NAMES.length)) {
                result.append(msg_start + ": " + IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
                int so = player.getStartOffset();
                int sw = player.getStartWidth();
                if ((so != 0) || (sw != 3)) {
                    result.append(", " + so);
                    result.append(", " + sw);
                }
            } else {
                String msg_none = Messages.getString("ChatLounge.None");
                result.append(msg_start + ": " + msg_none);
            }
            result.append("</FONT>");
            
            if (!LobbyUtility.isValidStartPos(lobby.game(), player)) {
                result.append(guiScaledFontHTML(uiYellow())); 
                result.append(WARNING_SIGN + "</FONT>");
            }
            
            // Player BV
            result.append(UIUtil.DOT_SPACER);
            result.append(guiScaledFontHTML());
            String msg_bvplain = Messages.getString("ChatLounge.BVplain");
            result.append(msg_bvplain + ": ");
            NumberFormat formatter = NumberFormat.getIntegerInstance(MegaMek.getMMOptions().getLocale());
            result.append((player.getBV() != 0) ? formatter.format(player.getBV()) : "--");
            result.append("</FONT>");

            // Initiative Mod
            if (player.getConstantInitBonus() != 0) {
                result.append(UIUtil.DOT_SPACER);
                result.append(guiScaledFontHTML());
                String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
                String msg_init = Messages.getString("ChatLounge.Init");
                result.append(msg_init + ": ").append(sign);
                result.append(player.getConstantInitBonus());
                result.append("</FONT>");
            }

            if (player.getSingleBlind()) {
                result.append(UIUtil.DOT_SPACER);
                result.append(guiScaledFontHTML(uiGreen()));
                result.append("\uD83D\uDC41");
                result.append("</FONT>");
            }

            setText(result.toString());

            setIconTextGap(scaleForGUI(10));
            Image camo = player.getCamouflage().getImage();
            int size = scaleForGUI(PLAYERTABLE_ROWHEIGHT) / 2;
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
