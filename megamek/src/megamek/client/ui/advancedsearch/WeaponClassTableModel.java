package megamek.client.ui.advancedsearch;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.Vector;

public class WeaponClassTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    static final int COL_QTY = 0;
    static final int COL_NAME = 1;
    static final int N_COL = 2;
    static final int COL_VAL = 2;


    private int[] qty;

    private Vector<WeaponClass> weaponClasses = new Vector<>();

    public WeaponClassTableModel() {
        for (WeaponClass cl : WeaponClass.values()) {
            weaponClasses.add(cl);
        }
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
        switch (col) {
            case COL_QTY:
                return 40;
            case COL_NAME:
                return 310;
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
                return "Weapon Class";
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
    // public void setData(Vector<Integer> wps) {
    //     weaponClasses = wps;
    //     qty = new int[wps.size()];
    //     Arrays.fill(qty, 1);
    //     fireTableDataChanged();
    // }

    public WeaponClass getWeaponTypeAt(int row) {
        return weaponClasses.elementAt(row);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= weaponClasses.size()) {
            return null;
        }

        switch (col) {
            case COL_QTY:
                return qty[row] + "";
            case COL_NAME:
                return weaponClasses.elementAt(row).toString();
            case COL_VAL:
                return weaponClasses.elementAt(row);
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
