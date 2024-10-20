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

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

public class WeaponClassTableModel extends AbstractTableModel {

    static final int COL_QTY = 0;
    static final int COL_NAME = 1;
    static final int N_COL = 2;
    static final int COL_VAL = 2;


    private final int[] qty;

    private final Vector<WeaponClass> weaponClasses = new Vector<>();

    public WeaponClassTableModel() {
        Collections.addAll(weaponClasses, WeaponClass.values());
        qty = new int[weaponClasses.size()];
        Arrays.fill(qty, 1);
    }

    @Override
    public int getRowCount() {
        return weaponClasses.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public int getPreferredWidth(int col) {
        return switch (col) {
            case COL_QTY -> 40;
            case COL_NAME -> 310;
            default -> 0;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_QTY -> "Qty";
            case COL_NAME -> "Weapon Class";
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

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= weaponClasses.size()) {
            return null;
        }

        return switch (col) {
            case COL_QTY -> qty[row] + "";
            case COL_NAME -> weaponClasses.elementAt(row).toString();
            case COL_VAL -> weaponClasses.elementAt(row);
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
