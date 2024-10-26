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
import megamek.common.MiscType;
import megamek.common.TechConstants;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A table model for the advanced search weapon tab's equipment list
 */
class EquipmentTableModel extends AbstractTableModel {

    static final int COL_NAME = 0;
    static final int COL_COST = 1;
    static final int COL_IS_CLAN = 2;
    static final int COL_LEVEL = 3;
    static final int N_COL = 4;
    static final int COL_INTERNAL_NAME = 5;

    private final TWAdvancedSearchPanel twAdvancedSearchPanel;
    private final List<MiscType> equipment = new ArrayList<>();

    EquipmentTableModel(TWAdvancedSearchPanel twAdvancedSearchPanel) {
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

    int getPreferredWidth(int col) {
        return switch (col) {
            case COL_NAME -> 400;
            case COL_IS_CLAN -> 75;
            case COL_COST -> 175;
            case COL_LEVEL -> 100;
            default -> 0;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> "Name";
            case COL_IS_CLAN -> "IS/Clan";
            case COL_COST -> "Cost";
            case COL_LEVEL -> "Lvl";
            default -> "?";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    void setData(List<MiscType> eq) {
        equipment.clear();
        equipment.addAll(eq);
        fireTableDataChanged();
    }

    MiscType getEquipmentTypeAt(int row) {
        return equipment.get(row);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= equipment.size()) {
            return null;
        }
        EquipmentType eq = equipment.get(row);
        return switch (col) {
            case COL_NAME -> eq.getName();
            case COL_IS_CLAN -> TechConstants.getTechName(eq.getTechLevel(twAdvancedSearchPanel.gameYear));
            case COL_COST -> eq.getRawCost();
            case COL_LEVEL -> TechConstants.getSimpleLevelName(
                TechConstants.convertFromNormalToSimple(eq.getTechLevel(twAdvancedSearchPanel.gameYear)));
            case COL_INTERNAL_NAME -> eq.getInternalName();
            default -> "?";
        };
    }
}
