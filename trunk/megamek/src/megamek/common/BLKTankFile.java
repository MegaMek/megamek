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
 *
 * @author  njrkrynn
 * @version 
 */
package megamek.common;

import java.io.*;

import megamek.common.util.*;

public class BLKTankFile implements MechLoader {    
    
    BuildingBlock dataFile;
    private static final String[] MOVES = { "", "", "", "Tracked", "Wheeled", "Hover" };
    
    /** Creates new BLkFile */
    public BLKTankFile(InputStream is) {
        
        dataFile = new BuildingBlock(is);
        
    }
    
    public BLKTankFile(BuildingBlock bb) {
        dataFile = bb;
    }
      
    //if it's a block file it should have this...
    public boolean isMine() {
     
        if (dataFile.exists("blockversion") ) return true;
        
        return false;
        
    }

    public Entity getEntity() throws EntityLoadingException {
    
        Tank t = new Tank();
        
        if (!dataFile.exists("name")) throw new EntityLoadingException("Could not find name block.");
        t.setChassis(dataFile.getDataAsString("Name")[0]);
        t.setModel(t.getChassis());
        
        if (!dataFile.exists("year")) throw new EntityLoadingException("Could not find year block.");
        t.setYear(dataFile.getDataAsInt("year")[0]);
            
        if (!dataFile.exists("type")) throw new EntityLoadingException("Could not find type block.");
            
        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (t.getYear() == 3025) {
                t.setTechLevel(TechConstants.T_IS_LEVEL_1);
            } else {
                t.setTechLevel(TechConstants.T_IS_LEVEL_2);
            }
        } else {
            t.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
        }
        
        if (!dataFile.exists("tonnage")) throw new EntityLoadingException("Could not find weight block.");
        t.weight = dataFile.getDataAsFloat("tonnage")[0];
            
        if (!dataFile.exists("motion_type")) throw new EntityLoadingException("Could not find movement block.");
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        for (int x = 0; x < MOVES.length; x++) {
            if (sMotion.equals(MOVES[x])) {
                nMotion = x;
                break;
            }
        }
        if (nMotion == -1) throw new EntityLoadingException("Invalid movment type: " + sMotion);
        t.setMovementType(nMotion);
        
        
        if (!dataFile.exists("cruiseMP")) throw new EntityLoadingException("Could not find cruiseMP block.");
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);
            
    
        if (!dataFile.exists("armor") ) throw new EntityLoadingException("Could not find armor block.");
        
        int[] armor = dataFile.getDataAsInt("armor");
        
        if (armor.length < 4 || armor.length > 5) {
            throw new EntityLoadingException("Incorrect armor array length");   
        }
        
        t.setHasTurret(armor.length == 5);
        
        // add the body to the armor array
        int[] fullArmor = new int[armor.length + 1];
        fullArmor[0] = 0;
        System.arraycopy(armor, 0, fullArmor, 1, armor.length);
        for (int x = 0; x < fullArmor.length; x++) {
            t.initializeArmor(fullArmor[x], x);
        }
        
        
        
        t.autoSetInternal();
        
        loadEquipment(t, "Front", Tank.LOC_FRONT);
        loadEquipment(t, "Right", Tank.LOC_RIGHT);
        loadEquipment(t, "Left", Tank.LOC_LEFT);
        loadEquipment(t, "Rear", Tank.LOC_REAR);
        if (t.hasTurret()) {
            loadEquipment(t, "Turret", Tank.LOC_TURRET);
        }
        loadEquipment(t, "Body", Tank.LOC_BODY);
        return t;        
    }
    
    private void loadEquipment(Tank t, String sName, int nLoc) 
            throws EntityLoadingException
    {            
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) return;
        
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
}

