/*
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
package megamek.client.ui.swing;

import java.awt.*;
import java.beans.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * The TableColumnManager can be used to manage TableColumns. It will give the
 * user the ability to hide columns and then reshow them in their last viewed
 * position. This functionality is supported by a popup menu added to the
 * table header of the table. The TableColumnModel is still used to control
 * the view for the table. The manager will invoke the appropriate methods
 * of the TableColumnModel to hide/show columns as required.
 *
 * Taken from: https://tips4java.wordpress.com/2011/05/08/table-column-manager/
 */
public class TableColumnManager implements MouseListener, ActionListener, TableColumnModelListener,
        PropertyChangeListener {
    private JTable table;
    private TableColumnModel tcm;
    private boolean menuPopup;

    private List<TableColumn> allColumns;

    /**
     * Create a TableColumnManager for a table.
     *
     * @param table the table whose TableColumns will be managed
     * @param menuPopup enable or disable a popup menu to allow the users to
     *                  manager the visibility of TableColumns.
     */
    public TableColumnManager(JTable table, boolean menuPopup) {
        this.table = table;
        setMenuPopup(menuPopup);

        table.addPropertyChangeListener(this);
        reset();
    }

    /**
     * Reset the TableColumnManager to only manage the TableColumns that are
     * currently visible in the table.
     *
     * Generally this method should only be invoked by the TableColumnManager
     * when the TableModel of the table is changed.
     */
    public void reset() {
        table.getColumnModel().removeColumnModelListener(this);
        tcm = table.getColumnModel();
        tcm.addColumnModelListener(this);

        // Keep a duplicate TableColumns for managing hidden TableColumns
        int count = tcm.getColumnCount();
        allColumns = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            allColumns.add(tcm.getColumn(i));
        }
    }

    /**
     * Add/remove support for a popup menu to the table header. The popup
     * menu will give the user control over which columns are visible.
     *
     * @param menuPopup when true support for displaying a popup menu is added
     *                  otherwise the popup menu is removed.
     */
    public void setMenuPopup(boolean menuPopup) {
        table.getTableHeader().removeMouseListener(this);

        if (menuPopup) {
            table.getTableHeader().addMouseListener(this);
        }

        this.menuPopup = menuPopup;
    }

    /**
     * Hide a column from view in the table.
     *
     * @param modelColumn the column index from the TableModel of the column to be removed
     */
    public void hideColumn(int modelColumn) {
        int viewColumn = table.convertColumnIndexToView(modelColumn);

        if (viewColumn != -1) {
            TableColumn column = tcm.getColumn(viewColumn);
            hideColumn(column);
        }
    }

    /**
     * Hide a column from view in the table.
     *
     * @param columnName the column name of the column to be removed
     */
    public void hideColumn(Object columnName) {
        if (columnName == null) {
            return;
        }

        for (int i = 0; i < tcm.getColumnCount(); i++) {
            TableColumn column = tcm.getColumn(i);

            if (columnName.equals(column.getHeaderValue())) {
                hideColumn(column);
                break;
            }
        }
    }

    /**
     * Hide a column from view in the table.
     *
     * @param column the TableColumn to be removed from the TableColumnModel of the table
     */
    public void hideColumn(TableColumn column) {
        if (tcm.getColumnCount() == 1) {
            return;
        }

        // Ignore changes to the TableColumnModel made by the TableColumnManager
        tcm.removeColumnModelListener(this);
        tcm.removeColumn(column);
        tcm.addColumnModelListener(this);
    }

    /**
     * Show a hidden column in the table.
     *
     * @param modelColumn the column index from the TableModel of the column to be added
     */
    public void showColumn(int modelColumn) {
        for (TableColumn column : allColumns) {
            if (column.getModelIndex() == modelColumn) {
                showColumn(column);
                break;
            }
        }
    }

    /**
     * Show a hidden column in the table.
     *
     * @param columnName the column name from the TableModel of the column to be added
     */
    public void showColumn(Object columnName) {
        for (TableColumn column : allColumns) {
            if (column.getHeaderValue().equals(columnName)) {
                showColumn(column);
                break;
            }
        }
    }

    /**
     * Show a hidden column in the table. The column will be positioned
     * at its proper place in the view of the table.
     *
     * @param column the TableColumn to be shown.
     */
    private void showColumn(TableColumn column) {
        // Ignore changes to the TableColumnModel made by the TableColumnManager
        tcm.removeColumnModelListener(this);

        // Add the column to the end of the table
        tcm.addColumn(column);

        // Move the column to its position before it was hidden.
        // (Multiple columns may be hidden so we need to find the first
        // visible column before this column so the column can be moved
        // to the appropriate position)
        int position = allColumns.indexOf(column);
        int from = tcm.getColumnCount() - 1;
        int to = 0;

        for (int i = position - 1; i > -1; i--) {
            try {
                TableColumn visibleColumn = allColumns.get(i);
                to = tcm.getColumnIndex(visibleColumn.getHeaderValue()) + 1;
                break;
            } catch (IllegalArgumentException ignored) {

            }
        }

        tcm.moveColumn(from, to);

        tcm.addColumnModelListener(this);
    }

    //region MouseListener
    @Override
    public void mousePressed(MouseEvent e) {
        checkForPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        checkForPopup(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private void checkForPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JTableHeader header = (JTableHeader) e.getComponent();
            int column = header.columnAtPoint(e.getPoint());
            showPopup(column);
        }
    }

    /**
     * Show a popup containing items for all the columns found in the
     * table column manager. The popup will be displayed below the table
     * header columns that was clicked.
     *
     * @param index index of the table header column that was clicked
     */
    private void showPopup(int index) {
        Object headerValue = tcm.getColumn(index).getHeaderValue();
        int columnCount = tcm.getColumnCount();
        JPopupMenu popup = new SelectPopupMenu();

        // Create a menu item for all columns managed by the table column
        // manager, checking to see if the column is shown or hidden.

        for (TableColumn tableColumn : allColumns) {
            Object value = tableColumn.getHeaderValue();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(value.toString());
            item.addActionListener( this);

            try {
                tcm.getColumnIndex(value);
                item.setSelected(true);

                if (columnCount == 1) {
                    item.setEnabled(false);
                }
            } catch (IllegalArgumentException e) {
                item.setSelected(false);
            }

            popup.add(item);

            if (value == headerValue) {
                popup.setSelected(item);
            }
        }

        // Display the popup below the TableHeader
        JTableHeader header = table.getTableHeader();
        Rectangle r = header.getHeaderRect(index);
        popup.show(header, r.x, r.height);
    }
    //endregion MouseListener

    //region ActionListener
    /**
     * A table column will either be added to the table or removed from the
     * table depending on the state of the menu item that was clicked.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) event.getSource();
            String column = event.getActionCommand();

            if (button.isSelected()) {
                showColumn(column);
            } else {
                hideColumn(column);
            }
        }
    }
    //endregion ActionListener

    //region TableColumnModelListener
    @Override
    public void columnAdded(TableColumnModelEvent e) {
        // A table column was added to the TableColumnModel so we need
        // to update the manager to track this column
        TableColumn column = tcm.getColumn( e.getToIndex() );

        if (!allColumns.contains(column)) {
            allColumns.add(column);
        }
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if (e.getFromIndex() == e.getToIndex()) {
            return;
        }

        //  A table column has been moved one position to the left or right
        //  in the view of the table so we need to update the manager to
        //  track the new location

        int index = e.getToIndex();
        TableColumn column = tcm.getColumn( index );
        allColumns.remove( column );

        if (index == 0) {
            allColumns.add(0, column);
        } else {
            index--;
            TableColumn visibleColumn = tcm.getColumn( index );
            int insertionColumn = allColumns.indexOf( visibleColumn );
            allColumns.add(insertionColumn + 1, column);
        }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {

    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {

    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {

    }
    //endregion TableColumnModelListener

    //region PropertyChangeListener
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if ("model".equals(e.getPropertyName())) {
            if (table.getAutoCreateColumnsFromModel()) {
                reset();
            }
        }
    }
    //endregion PropertyChangeListener

    /*
     * Allows you to select a specific menu item when the popup is
     * displayed. (ie. this is a bug? fix)
     */
    static class SelectPopupMenu extends JPopupMenu {
        private static final long serialVersionUID = 2603169517151855563L;

        @Override
        public void setSelected(Component sel) {
            int index = getComponentIndex(sel);
            getSelectionModel().setSelectedIndex(index);
            final MenuElement[] me = new MenuElement[2];
            me[0] = this;
            me[1] = getSubElements()[index];

            SwingUtilities.invokeLater(() -> MenuSelectionManager.defaultManager().setSelectedPath(me));
        }
    }
}
