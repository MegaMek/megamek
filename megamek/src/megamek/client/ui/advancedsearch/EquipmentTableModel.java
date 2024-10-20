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

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * A table model for displaying equipment
 */
public class EquipmentTableModel extends AbstractTableModel {

    static final int COL_QTY = 0;
    static final int COL_NAME = 1;
    static final int COL_COST = 2;
    static final int COL_IS_CLAN = 3;
    static final int COL_LEVEL = 4;
    static final int N_COL = 5;
    static final int COL_INTERNAL_NAME = 5;

    private final TWAdvancedSearchPanel twAdvancedSearchPanel;
    private int[] qty;
    private Vector<EquipmentType> equipment = new Vector<>();

    public EquipmentTableModel(TWAdvancedSearchPanel twAdvancedSearchPanel) {
        this.twAdvancedSearchPanel = twAdvancedSearchPanel;
    }

    @Override
    public int getRowCount() {
        return equipment.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public int getPreferredWidth(int col) {
        switch (col) {
            case COL_QTY:
                return 40;
            case COL_NAME:
                return 400;
            case COL_IS_CLAN:
                return 75;
            case COL_COST:
                return 175;
            case COL_LEVEL:
                return 100;
            default:
                return 0;
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_QTY:
                return "Qty";
            case COL_NAME:
                return "Name";
            case COL_IS_CLAN:
                return "IS/Clan";
            case COL_COST:
                return "Cost";
            case COL_LEVEL:
                return "Lvl";
            default:
                return "?";
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        switch (col) {
            case COL_QTY:
                return true;
            default:
                return false;
        }
    }

    // fill table with values
    public void setData(Vector<EquipmentType> eq) {
        equipment = eq;
        qty = new int[eq.size()];
        Arrays.fill(qty, 1);
        fireTableDataChanged();
    }

    public EquipmentType getEquipmentTypeAt(int row) {
        return equipment.elementAt(row);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= equipment.size()) {
            return null;
        }
        EquipmentType eq = equipment.elementAt(row);
        switch (col) {
            case COL_QTY:
                return qty[row] + "";
            case COL_NAME:
                return eq.getName();
            case COL_IS_CLAN:
                return TechConstants.getTechName(eq.getTechLevel(twAdvancedSearchPanel.gameYear));
            case COL_COST:
                return eq.getRawCost();
            case COL_LEVEL:
                return TechConstants.getSimpleLevelName(TechConstants
                    .convertFromNormalToSimple(eq
                        .getTechLevel(twAdvancedSearchPanel.gameYear)));
            case COL_INTERNAL_NAME:
                return eq.getInternalName();
            default:
                return "?";
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        switch (col) {
            case COL_QTY:
                qty[row] = Integer.parseInt((String) value);
                fireTableCellUpdated(row, col);
                break;
            default:
                break;
        }
    }
}
