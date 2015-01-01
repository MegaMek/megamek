/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * This class loads Infantry BLK files.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.MiscType;
import megamek.common.util.BuildingBlock;

public class BLKInfantryFile extends BLKFile implements IMechLoader {

    // HACK!!! Infantry movement reuses Mech and Vehicle movement.
    private static final String[] MOVES = { "", "", "", "Tracked", "Wheeled", "Hover", "", "", "", "", "Leg", "Motorized", "Jump", "", "", "", "", "", "Submarine" };

    public BLKInfantryFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        Infantry t = new Infantry();

        if (!dataFile.exists("name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        t.setChassis(dataFile.getDataAsString("Name")[0]);

        if (!dataFile.exists("model")) {
            throw new EntityLoadingException("Could not find model block.");
        }
        t.setModel(dataFile.getDataAsString("Model")[0]);

        setTechLevel(t);

        if (dataFile.exists("source")) {
            t.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        for (int x = 0; x < MOVES.length; x++) {
            if (sMotion.equals(MOVES[x])) {
                nMotion = x;
                break;
            }
        }
        if (nMotion == -1) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);

        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP")) {
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
        }

        loadEquipment(t, "Platoon", Infantry.LOC_INFANTRY);

        if (dataFile.exists("troopers")) {
            int troopers = dataFile.getDataAsInt("troopers")[0];
            t.initializeInternal(troopers, Infantry.LOC_INFANTRY);
            if (t.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
                t.initializeArmor(troopers, Infantry.LOC_INFANTRY);
            }
        } else {
            t.autoSetInternal();
        }

        return t;
    }

}
