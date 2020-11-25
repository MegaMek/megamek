package megamek.client.ui.swing.lobby;

import megamek.common.Entity;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/** A Lobby Mek Table sorter that sorts by tonnage. */
public class TonnageSorter implements MekTableSorter {
    
    private Sorting direction;
    
    /** A Lobby Mek Table sorter that sorts by tonnage. */
    public TonnageSorter(MekTableSorter.Sorting dir) {
        direction = dir;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        double aWeight = a.getWeight();
        double bWeight = b.getWeight();
        if (bWeight > aWeight) {
            return smaller(direction);
        } else if (bWeight < aWeight) {
            return bigger(direction);
        } else {
            return 0;
        }
    }

    @Override
    public String getDisplayName() {
        return "Tonnage";
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }
    
    @Override
    public boolean isAllowed(GameOptions opts) {
        return !opts.booleanOption(OptionsConstants.BASE_BLIND_DROP);
    }

    @Override
    public Sorting getSortingDirection() {
        return direction;
    }

}
