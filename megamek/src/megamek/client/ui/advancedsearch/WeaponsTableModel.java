/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.advancedsearch;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * A table model for displaying weapons
 */
public class WeaponsTableModel extends AbstractTableModel {

    static final int COL_QTY = 0;
    static final int COL_NAME = 1;
    static final int COL_DMG = 2;
    static final int COL_HEAT = 3;
    static final int COL_SHORT = 4;
    static final int COL_MED = 5;
    static final int COL_LONG = 6;
    static final int COL_IS_CLAN = 7;
    static final int COL_LEVEL = 8;
    static final int N_COL = 9;
    static final int COL_INTERNAL_NAME = 9;

    private final TWAdvancedSearchPanel twAdvancedSearchPanel;
    private int[] qty;

    private Vector<WeaponType> weapons = new Vector<>();

    public WeaponsTableModel(TWAdvancedSearchPanel twAdvancedSearchPanel) {
        this.twAdvancedSearchPanel = twAdvancedSearchPanel;
    }

    @Override
    public int getRowCount() {
        return weapons.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public int getPreferredWidth(int col) {
        return switch (col) {
            case COL_QTY -> 40;
            case COL_NAME -> 310;
            case COL_IS_CLAN -> 75;
            case COL_DMG, COL_HEAT, COL_SHORT, COL_MED, COL_LONG -> 50;
            case COL_LEVEL -> 100;
            default -> 0;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_QTY -> "Qty";
            case COL_NAME -> "Weapon Name";
            case COL_IS_CLAN -> "IS/Clan";
            case COL_DMG -> "DMG";
            case COL_HEAT -> "Heat";
            case COL_SHORT -> "Short";
            case COL_MED -> "Med";
            case COL_LONG -> "Long";
            case COL_LEVEL -> "Lvl";
            default -> "?";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == COL_QTY;
    }

    // fill table with values
    public void setData(Vector<WeaponType> wps) {
        weapons = wps;
        qty = new int[wps.size()];
        Arrays.fill(qty, 1);
        fireTableDataChanged();
    }

    public WeaponType getWeaponTypeAt(int row) {
        return weapons.elementAt(row);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= weapons.size()) {
            return null;
        }
        WeaponType wp = weapons.elementAt(row);
        return switch (col) {
            case COL_QTY -> qty[row] + "";
            case COL_NAME -> wp.getName();
            case COL_IS_CLAN -> TechConstants.getTechName(wp.getTechLevel(twAdvancedSearchPanel.gameYear));
            case COL_DMG -> wp.getDamage();
            case COL_HEAT -> wp.getHeat();
            case COL_SHORT -> wp.getShortRange();
            case COL_MED -> wp.getMediumRange();
            case COL_LONG -> wp.getLongRange();
            case COL_LEVEL -> TechConstants.getSimpleLevelName(
                TechConstants.convertFromNormalToSimple(wp.getTechLevel(twAdvancedSearchPanel.gameYear)));
            case COL_INTERNAL_NAME -> wp.getInternalName();
            default -> "?";
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == COL_QTY) {
            qty[row] = Integer.parseInt((String) value);
            fireTableCellUpdated(row, col);
        }
    }
}
