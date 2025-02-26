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

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

class SearchableTable extends JTable {

    private static final int KEY_TIMEOUT = 1000;

    private long lastSearch;
    StringBuffer searchBuffer;

    /**
     * Determines which column in the table model will be used for searches.
     */
    protected int searchColumn;

    public SearchableTable() {
        super();
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(TableModel dm) {
        super(dm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(TableModel dm, int sc) {
        super(dm);
        lastSearch = 0;
        searchColumn = sc;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(TableModel dm, TableColumnModel cm,
                           ListSelectionModel sm) {
        super(dm, cm, sm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public SearchableTable(Vector<Vector<String>> rowData, Vector<String> columnNames) {
        super(rowData, columnNames);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public int getSearchColumn() {
        return searchColumn;
    }

    public void setSearchColumn(int searchColumn) {
        this.searchColumn = searchColumn;
    }

    /**
     * getToolTipText method that implements cell tooltips. This is useful for
     * displaying cells that are larger than the column width
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        if (rowIndex >= 0) {
            tip = getValueAt(rowIndex, colIndex).toString();
        }
        return tip;
    }

    public void keyTyped(KeyEvent ke) {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }
        lastSearch = curTime;
        searchBuffer.append(ke.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }

    /**
     * When keys are pressed with focus on this table, they are added to a
     * search buffer, which is then used to search on a predetrmined column for
     * selection.
     *
     * @param search
     */
    protected void searchFor(String search) {
        int rows = getRowCount();
        for (int row = 0; row < rows; row++) {
            String name = (String) getValueAt(row, searchColumn);
            if (name.toLowerCase().startsWith(search)) {
                changeSelection(row, 0, false, false);
                break;
            }
        }
    }
}
