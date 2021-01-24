package megamek.client.ui.swing.lobby.sorters;

import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;

/** A Lobby Mek Table sorter that sorts by unit ID. */
public class IDSorter implements MekTableSorter {
    
    private Sorting direction;
    
    /** A Lobby Mek Table sorter that sorts by unit ID. */
    public IDSorter(MekTableSorter.Sorting dir) {
        direction = dir;
    }
    
    @Override
    public int compare(final Entity a, final Entity b) {
        int aVal = a.getId();
        int bVal = b.getId();
        if (bVal > aVal) {
            return smaller(direction);
        } else if (bVal < aVal) {
            return bigger(direction);
        } else {
            return 0;
        }
    }

    @Override
    public String getDisplayName() {
        return "ID";
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }
    
    @Override
    public Sorting getSortingDirection() {
        return direction;
    }

}
