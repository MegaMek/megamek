package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.TechConstants;
import megamek.common.util.BuildingBlock;

public class BLKFile {

    BuildingBlock dataFile;

    protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null)
            return;

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        for (int x = 0; x < saEquip.length; x++) {
            String equipName = saEquip[x].trim();
            EquipmentType etype = EquipmentType.getByMtfName(equipName);

            if (etype == null) {
                etype = EquipmentType.getByMepName(equipName);
            }

            if (etype == null) {
                // try w/ prefix
                etype = EquipmentType.getByMepName(prefix + equipName);
            }

            if (etype != null) {
                try {
                    t.addEquipment(etype, nLoc);
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
        }
    }

    public boolean isMine() {
    
        if (dataFile.exists("blockversion") ) return true;
        
        return false;
        
    }
}
