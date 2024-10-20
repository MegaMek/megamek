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
        switch (col) {
            case COL_QTY:
                return 40;
            case COL_NAME:
                return 310;
            case COL_IS_CLAN:
                return 75;
            case COL_DMG:
                return 50;
            case COL_HEAT:
                return 50;
            case COL_SHORT:
                return 50;
            case COL_MED:
                return 50;
            case COL_LONG:
                return 50;
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
                return "Weapon Name";
            case COL_IS_CLAN:
                return "IS/Clan";
            case COL_DMG:
                return "DMG";
            case COL_HEAT:
                return "Heat";
            case COL_SHORT:
                return "Short";
            case COL_MED:
                return "Med";
            case COL_LONG:
                return "Long";
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
        switch (col) {
            case COL_QTY:
                return qty[row] + "";
            case COL_NAME:
                return wp.getName();
            case COL_IS_CLAN:
                return TechConstants.getTechName(wp.getTechLevel(twAdvancedSearchPanel.gameYear));
            case COL_DMG:
                return wp.getDamage();
            case COL_HEAT:
                return wp.getHeat();
            case COL_SHORT:
                return wp.getShortRange();
            case COL_MED:
                return wp.getMediumRange();
            case COL_LONG:
                return wp.getLongRange();
            case COL_LEVEL:
                return TechConstants.getSimpleLevelName(TechConstants
                    .convertFromNormalToSimple(wp
                        .getTechLevel(twAdvancedSearchPanel.gameYear)));
            case COL_INTERNAL_NAME:
                return wp.getInternalName();
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
