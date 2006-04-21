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
import megamek.common.TechConstants;
import megamek.common.util.*;

public class BLKInfantryFile extends BLKFile implements IMechLoader {    
    
    // HACK!!!  Infantry movement reuses Mech and Vehicle movement.
    private static final String[] MOVES = { "","","","","","","","","","", "Leg","Motorized","Jump" };
        
    public BLKInfantryFile(BuildingBlock bb) {
        dataFile = bb;
    }
      
    public Entity getEntity() throws EntityLoadingException {
    
        Infantry t = new Infantry();
        
        if (!dataFile.exists("name")) throw new EntityLoadingException("Could not find name block.");
        t.setChassis(dataFile.getDataAsString("Name")[0]);
        
        if (!dataFile.exists("model")) throw new EntityLoadingException("Could not find model block.");
            t.setModel(dataFile.getDataAsString("Model")[0]);
        
        if (!dataFile.exists("year")) throw new EntityLoadingException("Could not find year block.");
        t.setYear(dataFile.getDataAsInt("year")[0]);
            
        if (!dataFile.exists("type")) throw new EntityLoadingException("Could not find type block.");
            
        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (t.getYear() == 3025) {
                t.setTechLevel(TechConstants.T_IS_LEVEL_1);
            } else {
                t.setTechLevel(TechConstants.T_IS_LEVEL_2);
            }
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 1")) {
            t.setTechLevel(TechConstants.T_IS_LEVEL_1);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 2")) {
            t.setTechLevel(TechConstants.T_IS_LEVEL_2);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 3")) {
            t.setTechLevel(TechConstants.T_IS_LEVEL_3);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan")) {
            t.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 3")) {
            t.setTechLevel(TechConstants.T_CLAN_LEVEL_3);
        }
        
        if (!dataFile.exists("tonnage")) throw new EntityLoadingException("Could not find weight block.");
        t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);
            
        if (!dataFile.exists("motion_type")) throw new EntityLoadingException("Could not find movement block.");
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        for (int x = 0; x < MOVES.length; x++) {
            if (sMotion.equals(MOVES[x])) {
                nMotion = x;
                break;
            }
        }
        if (nMotion == -1) throw new EntityLoadingException("Invalid movement type: " + sMotion);
        t.setMovementMode(nMotion);

        if (!dataFile.exists("cruiseMP")) throw new EntityLoadingException("Could not find cruiseMP block.");
        t.setOriginalRunMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP"))
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);

        loadEquipment(t, "Platoon", Infantry.LOC_INFANTRY);

        t.autoSetInternal();
        
        return t;        
    }
    
}

