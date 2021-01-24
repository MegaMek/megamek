package megamek.client.ui.swing.lobby.sorters;

import java.util.Comparator;

import megamek.common.Entity;
import megamek.common.options.GameOptions;

/** An interface for the Comparators used for the lobby Mek table. */
public interface MekTableSorter extends Comparator<Entity> {
    
    public enum Sorting { ASCENDING, DESCENDING; }

    /** 
     * Returns the info that is displayed in the column header to show
     * the sorting that is used, such as "Team / BV".
     */
    String getDisplayName();
    
    /** 
     * Returns the column index of the Mek Table that this sorter is to be used with. 
     */
    int getColumnIndex();
    
    /**
     * Returns true if this Sorter is currently allowed. Sorters might not be allowed e.g.
     * when they would give away info in blind drops.
     */
    default boolean isAllowed(GameOptions opts) {
        return true;
    }
    
    /** Returns the sorting direction. */
    default Sorting getSortingDirection() {
        return null;
    };
    
    /** Returns 1 if dir is ASCENDING, -1 otherwise. */
    default int bigger(Sorting dir) {
        if (dir == Sorting.ASCENDING) {
            return 1;
        } else {
            return -1;
        }
    }
    
    /** Returns -1 if dir is ASCENDING, 1 otherwise. */
    default int smaller(Sorting dir) {
        if (dir == Sorting.ASCENDING) {
            return -1;
        } else {
            return 1;
        }
    }
    
}
