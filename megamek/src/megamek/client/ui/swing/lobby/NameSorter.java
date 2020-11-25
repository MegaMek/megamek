package megamek.client.ui.swing.lobby;

import megamek.common.Entity;

/** A Lobby Mek Table sorter that sorts by unit name. */
public class NameSorter implements MekTableSorter {

    private Sorting direction;

    /** A Lobby Mek Table sorter that sorts by unit name. */
    public NameSorter(Sorting dir) {
        direction = dir;
    }
    
    @Override
    public int compare(final Entity a, final Entity b) {
        String aVal = a.getDisplayName();
        String bVal = b.getDisplayName();
        if (direction == Sorting.ASCENDING) {
            return aVal.compareTo(bVal);
        } else {
            return bVal.compareTo(aVal);
        }
    }
    
    @Override
    public String getDisplayName() {
        return "Unit Name";
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
