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

/*
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author njrkrynn
 * @version
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.util.BuildingBlock;

public class BLKGunEmplacementFile extends BLKFile implements IMechLoader {

    public BLKGunEmplacementFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        GunEmplacement e = new GunEmplacement();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        e.setChassis(dataFile.getDataAsString("Name")[0]);

        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            e.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            e.setModel("");
        }

        setTechLevel(e);
        setFluff(e);
        checkManualBV(e);

        if (dataFile.exists("source")) {
            e.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (dataFile.exists("Turret")) {
            if (dataFile.getDataAsInt("Turret")[0] != 1) {
                e.setHasNoTurret(true);
            }
        }

        loadEquipment(e, "Guns", GunEmplacement.LOC_GUNS);
        e.setArmorTonnage(e.getArmorWeight());
        return e;
    }
}
