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
    // equipment flags (okay, like every type of equipment has its own flag)
    // TODO: l2 equipment flags
    public static final int     F_HEAT_SINK         = 0x0001;
    public static final int     F_DOUBLE_HEAT_SINK  = 0x0002;
    public static final int     F_JUMP_JET          = 0x0004;
    public static final int     F_CLUB              = 0x0008;
    public static final int     F_HATCHET           = 0x0010;
    public static final int     F_TREE_CLUB         = 0x0020;
    public static final int     F_CASE              = 0x0040;
    
    /** Creates new MiscType */
    public MiscType() {
        ;
    }
    
    
    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET)) {
            if (entity.getWeight() >= 55.0) {
                return 0.5f;
            } else if (entity.getWeight() >= 85.0) {
                return 1.0f;
            } else {
                return 2.0f;
            }
        } else if (hasFlag(F_HATCHET)) {
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
        if (hasFlag(F_HATCHET)) {
            return (int)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_DOUBLE_HEAT_SINK) && entity.getTechLevel() != TechConstants.T_CLAN_LEVEL_2) {
            return 3;
        }
        // right, well I'll just guess then
        return 1;
    }
    
    public float getBV(Entity entity) {
        if (bv != BV_VARIABLE) {
            return bv;
        }
        // check for known formulas
        if (hasFlag(F_HATCHET)) {
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
        EquipmentType.addType(createHeatSink());
        EquipmentType.addType(createJumpJet());
        EquipmentType.addType(createTreeClub());
        EquipmentType.addType(createGirderClub());
        EquipmentType.addType(createLimbClub());
        EquipmentType.addType(createHatchet());
        
        // Start of Level2 stuff
        EquipmentType.addType(createDoubleHeatSink());
        EquipmentType.addType(createISCASE());
        EquipmentType.addType(createCLCASE());
    }
    
    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Heat Sink";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_HEAT_SINK;
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
        misc.flags |= F_JUMP_JET;
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
        misc.flags |= F_TREE_CLUB | F_CLUB;
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
        misc.flags |= F_CLUB;
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
        misc.flags |= F_CLUB;
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
        misc.flags |= F_HATCHET;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }
    
    // Start of Level2 stuff
    
    // REMOVE ME WHEN HMPREAD IS UPDATED!
    public static MiscType createDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "REMOVE MEEE!!";
        misc.mepName = "REMOVE ME!";
        misc.mtfName = "Double Heat Sink";
        misc.tonnage = 1.0f;
        misc.criticals = CRITICALS_VARIABLE;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "ISDouble Heat Sink";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "CLDouble Heat Sink";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.internalName = "ISCASE";
        misc.mepName = misc.name;
        misc.mtfName = "ISCASE";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.flags |= F_CASE;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.internalName = "CLCASE";
        misc.mepName = misc.name;
        misc.mtfName = "CLCASE";
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.flags |= F_CASE;
        misc.bv = 0;
        
        return misc;
    }
    
}
