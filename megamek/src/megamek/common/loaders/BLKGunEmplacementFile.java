/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org) Copyright (C)
 * 2005 Mike Gratton <mike@vee.net>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.util.BuildingBlock;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKGunEmplacementFile extends BLKFile implements IMechLoader {

    public BLKGunEmplacementFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        GunEmplacement e = new GunEmplacement();
        setBasicEntityData(e);

        if (dataFile.exists("Turret")) {
            if (dataFile.getDataAsInt("Turret")[0] != 1) {
                e.setHasNoTurret(true);
            }
        }
        
        // our gun emplacements do not support dual turrets at this time
        e.setHasNoDualTurret(true);

        loadEquipment(e, "Guns", GunEmplacement.LOC_GUNS);
        e.setArmorTonnage(e.getArmorWeight());
        loadQuirks(e);
        return e;
    }
}
