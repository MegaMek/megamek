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

import static megamek.client.ui.swing.util.UIUtil.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import megamek.client.bot.BotClient;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

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
        column.setHeaderValue("Players");
    }

    void rescale() {
        setRowHeight(UIUtil.scaleForGUI(PLAYERTABLE_ROWHEIGHT));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        IPlayer player = model.getPlayerAt(rowAtPoint(p));
        if (player == null) {
            return null;
        }

        StringBuilder result = new StringBuilder("<HTML>");
        result.append(guiScaledFontHTML(player.getColour().getColour()));
        result.append(player.getName() + "</FONT>");

        result.append(guiScaledFontHTML());
        if ((lobby.client() instanceof BotClient) && player.equals(lobby.localPlayer())) {
            result.append(" (" + UIUtil.BOT_MARKER + " This Bot)");
        } else if (lobby.client().bots.containsKey(player.getName())) {
            result.append(" (" + UIUtil.BOT_MARKER + " Your Bot)");
        } else if (lobby.localPlayer().equals(player)) {
            result.append(" (You)");
        }
        result.append("<BR>");
        if (player.getConstantInitBonus() != 0) {
            String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
            result.append("Initiative Modifier: ").append(sign);
            result.append(player.getConstantInitBonus());
        } else {
            result.append("No Initiative Modifier");
        }
        if (lobby.game().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            int mines = player.getNbrMFConventional() + player.getNbrMFActive() 
            + player.getNbrMFInferno() + player.getNbrMFVibra();
            result.append("<BR>Total Minefields: ").append(mines);
        }
        return result.toString();
    }

    public static class PlayerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1372393680232901923L;

        static final int COL_PLAYER = 0;
        static final int N_COL = 1;

        private ArrayList<IPlayer> players;

        public PlayerTableModel() {
            players = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return players.size();
        }

        void replaceData(List<IPlayer> newPlayers) {
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

        IPlayer getPlayerAt(int row) {
            return players.get(row);
        }
    }

    class PlayerRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
        private static final long serialVersionUID = 4947299735765324311L;

        public PlayerRenderer() {
            setLayout(new GridLayout(1,1,5,0));
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        }

        private void setImage(Image img) {
            setIcon(new ImageIcon(img));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            IPlayer player = (IPlayer) value;

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
            result.append(IPlayer.teamNames[player.getTeam()]);
            result.append("</FONT>");

            // Deployment Position
            result.append(UIUtil.DOT_SPACER);
            result.append(guiScaledFontHTML());
            if ((player.getStartingPos() >= 0) && (player.getStartingPos() <= IStartingPositions.START_LOCATION_NAMES.length)) {
                result.append("Start: " + IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
            } else {
                result.append("Start: None");
            }
            result.append("</FONT>");
            
            if (!LobbyUtility.isValidStartPos(lobby.game(), player)) {
                result.append(guiScaledFontHTML(uiYellow())); 
                result.append(WARNING_SIGN + "</FONT>");
            }
            
            // Player BV
            result.append(UIUtil.DOT_SPACER);
            result.append(guiScaledFontHTML());
            result.append("BV: ");
            NumberFormat formatter = NumberFormat.getIntegerInstance(PreferenceManager.getClientPreferences().getLocale());
            result.append((player.getBV() != 0) ? formatter.format(player.getBV()) : "--");
            result.append("</FONT>");

            // Initiative Mod
            if (player.getConstantInitBonus() != 0) {
                result.append(UIUtil.DOT_SPACER);
                result.append(guiScaledFontHTML());
                String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
                result.append("Init: ").append(sign);
                result.append(player.getConstantInitBonus());
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