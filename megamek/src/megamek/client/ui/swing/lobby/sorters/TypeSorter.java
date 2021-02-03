package megamek.client.ui.swing.lobby.sorters;

import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;

/** A Lobby Mek Table sorter that sorts by unit type. */
public class TypeSorter implements MekTableSorter {
    
    @Override
    public int compare(final Entity a, final Entity b) {
        String aType = Entity.getEntityMajorTypeName(a.getEntityType());
        String bType = Entity.getEntityMajorTypeName(b.getEntityType());
        return aType.compareTo(bType);
    }

    @Override
    public String getDisplayName() {
        return "Unit Type";
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

}
