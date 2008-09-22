/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
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

/*
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author  njrkrynn
 * @version 
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.TechConstants;
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

        if (dataFile.exists("Model")
                && dataFile.getDataAsString("Model")[0] != null) {
            e.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            e.setModel("");
        }

        if (!dataFile.exists("Year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        e.setYear(dataFile.getDataAsInt("Year")[0]);

        if (!dataFile.exists("Type")) {
            throw new EntityLoadingException("Could not find type block.");
        }
        if (dataFile.getDataAsString("Type")[0].equals("IS")) {
            if (e.getYear() == 3025) {
                e.setTechLevel(TechConstants.T_INTRO_BOXSET);
            } else {
                e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
            }
        } else if (dataFile.getDataAsString("Type")[0].equals("IS Level 1")) {
            e.setTechLevel(TechConstants.T_INTRO_BOXSET);
        } else if (dataFile.getDataAsString("Type")[0].equals("IS Level 2")) {
            e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
        } else if (dataFile.getDataAsString("Type")[0].equals("IS Level 3")) {
            e.setTechLevel(TechConstants.T_IS_ADVANCED);
        } else if (dataFile.getDataAsString("Type")[0].equals("Clan")
                || dataFile.getDataAsString("type")[0].equals("Clan Level 2")) {
            e.setTechLevel(TechConstants.T_CLAN_TW);
        } else if (dataFile.getDataAsString("Type")[0].equals("Clan Level 3")) {
            e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
        } else if (dataFile.getDataAsString("Type")[0]
                .equals("Mixed (IS Chassis)")) {
            e.setTechLevel(TechConstants.T_IS_ADVANCED);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("Type")[0]
                .equals("Mixed (Clan Chassis)")) {
            e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("Type")[0].equals("Mixed")) {
            throw new EntityLoadingException(
                    "Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech level: "
                    + dataFile.getDataAsString("Type")[0]);
        }

        if (!dataFile.exists("ConstructionFactor")) {
            throw new EntityLoadingException("Could not find block.");
        }
        e
                .initConstructionFactor(dataFile
                        .getDataAsInt("ConstructionFactor")[0]);

        if (dataFile.exists("Height")) {
            e.setHeight(dataFile.getDataAsInt("Height")[0]);
        }

        if (dataFile.exists("Turret")) {
            e.setTurret(true);
            e.initTurretArmor(dataFile.getDataAsInt("Turret")[0]);
        }

        loadEquipment(e, "North", GunEmplacement.LOC_NORTH);
        loadEquipment(e, "East", GunEmplacement.LOC_EAST);
        loadEquipment(e, "West", GunEmplacement.LOC_WEST);
        if (e.hasTurret()) {
            loadEquipment(e, "Turret", GunEmplacement.LOC_TURRET);
        }
        loadEquipment(e, "Building", GunEmplacement.LOC_BUILDING);
        return e;
    }
}
