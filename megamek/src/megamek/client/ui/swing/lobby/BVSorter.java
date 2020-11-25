package megamek.client.ui.swing.lobby;

import megamek.common.Entity;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/** A Lobby Mek Table sorter that sorts by BV. */
public class BVSorter implements MekTableSorter {

    private Sorting direction;

    /** A Lobby Mek Table sorter that sorts by BV. */
    public BVSorter(MekTableSorter.Sorting dir) {
        direction = dir;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        int aBV = a.calculateBattleValue();
        int bBV = b.calculateBattleValue();
        if (bBV > aBV) {
            return smaller(direction);
        } else if (bBV < aBV) {
            return bigger(direction);
        } else {
            return 0;
        }
    }

    @Override
    public String getDisplayName() {
        return "BV";
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_BV;
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
