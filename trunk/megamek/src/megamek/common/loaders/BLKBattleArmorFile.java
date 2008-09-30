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
 * This class loads BattleArmor BLK files.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
package megamek.common.loaders;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.util.BuildingBlock;

public class BLKBattleArmorFile extends BLKFile implements IMechLoader {

    public BLKBattleArmorFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        BattleArmor t = new BattleArmor();

        if (!dataFile.exists("name"))
            throw new EntityLoadingException("Could not find name block.");
        t.setChassis(dataFile.getDataAsString("Name")[0]);

        // Model is not strictly necessary.
        if (dataFile.exists("Model")
                && dataFile.getDataAsString("Model")[0] != null) {
            t.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            t.setModel("");
        }
        
        setTechLevel(t);

        if (!dataFile.exists("tonnage"))
            throw new EntityLoadingException("Could not find weight block.");
        t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

        if (!dataFile.exists("BV"))
            throw new EntityLoadingException("Could not find BV block.");
        t.setBattleValue(dataFile.getDataAsInt("BV")[0]);

        if (!dataFile.exists("motion_type"))
            throw new EntityLoadingException("Could not find movement block.");
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        if (sMotion.equalsIgnoreCase("leg"))
            nMotion = IEntityMovementMode.INF_LEG;
        else if (sMotion.equalsIgnoreCase("jump"))
            nMotion = IEntityMovementMode.INF_JUMP;
        else if (sMotion.equalsIgnoreCase("vtol"))
            nMotion = IEntityMovementMode.VTOL;
        else if (sMotion.equalsIgnoreCase("submarine"))
            nMotion = IEntityMovementMode.INF_UMU;
        if (nMotion == -1)
            throw new EntityLoadingException("Invalid movement type: "
                    + sMotion);
        t.setMovementMode(nMotion);

        if (!dataFile.exists("cruiseMP"))
            throw new EntityLoadingException("Could not find cruiseMP block.");
        t.setOriginalRunMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP"))
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);

        if (!dataFile.exists("armor"))
            throw new EntityLoadingException("Could not find armor block.");

        int[] armor = dataFile.getDataAsInt("armor");

        // Each trooper has the same amount of armor
        if (armor.length != 1) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        // add the body to the armor array
        t.refreshLocations();
        for (int x = 1; x < t.locations(); x++) {
            t.initializeArmor(armor[0], x);
        }

        t.autoSetInternal();

        loadEquipment(t, "Squad", BattleArmor.LOC_SQUAD);
        String[] abbrs = t.getLocationAbbrs();
        for (int loop = 1; loop < t.locations(); loop++) {
            loadEquipment(t, abbrs[loop], loop);
        }
        return t;
    }
}
