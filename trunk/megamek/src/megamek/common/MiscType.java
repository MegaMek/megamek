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
 * MiscType.java
 *
 * Created on April 2, 2002, 12:15 PM
 */

package megamek.common;

/**
 *
 * @author  Ben
 * @version 
 */
public class MiscType extends EquipmentType {
    // some static links to these for convenience
    public final static MiscType HEAT_SINK = createHeatSink();
    public final static MiscType JUMP_JET = createJumpJet();

    /** Creates new MiscType */
    public MiscType() {
        ;
    }
    
    
    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (internalName.equals("Jump Jet")) {
            if (entity.getWeight() >= 55.0) {
                return 0.5f;
            } else if (entity.getWeight() >= 85.0) {
                return 1.0f;
            } else {
                return 2.0f;
            }
        } else if (internalName.equals("Hatchet")) {
            return (float)Math.ceil(entity.getWeight() / 15.0);
        }
        // okay, I'm out of ideas
        return 1.0f;
    }

    public int getCriticals(Entity entity) {
        if (criticals != CRITICALS_VARIABLE) {
            return criticals;
        }
        // check for known formulas
        if (internalName.equals("Hatchet")) {
            return (int)Math.ceil(entity.getWeight() / 15.0);
        }
        // right, well I'll just guess then
        return 1;
    }
    
    public float getBV(Entity entity) {
        if (bv != BV_VARIABLE) {
            return bv;
        }
        // check for known formulas
        if (internalName.equals("Hatchet")) {
            return (float)Math.ceil(entity.getWeight() / 15.0);
        }
        // maybe it's 0
        return 0;
    }
    
    
    /**
     * Add all the types of misc eq we can create to the list
     */
    public static void initializeTypes() {
        // all tech level 1 stuff
        EquipmentType.addType(HEAT_SINK);
        EquipmentType.addType(JUMP_JET);
        EquipmentType.addType(createTreeClub());
        EquipmentType.addType(createGirderClub());
        EquipmentType.addType(createLimbClub());
        EquipmentType.addType(createHatchet());
    }

    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Heat Sink";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createJumpJet() {
        MiscType misc = new MiscType();
        
        misc.name = "Jump Jet";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createTreeClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Tree Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createGirderClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Girder Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createLimbClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Limb Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createHatchet() {
        MiscType misc = new MiscType();
        
        misc.name = "Hatchet";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }

}
