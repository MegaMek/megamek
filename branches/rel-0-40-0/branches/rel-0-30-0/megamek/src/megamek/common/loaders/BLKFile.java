/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

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
            EquipmentType etype = EquipmentType.get(equipName);

            if (etype == null) {
                // try w/ prefix
                etype = EquipmentType.get(prefix + equipName);
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
