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
    public static final int     F_MASC              = 0x0080;
    public static final int     F_TSM               = 0x0100;
// + HentaiZonga
    public static final int     F_C3M               = 0x0200;
    public static final int     F_C3S               = 0x0400;
    public static final int     F_C3I               = 0x0800;

    public static final int     F_ECM               = 0x1000;
// - HentaiZonga
    
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
        } else if (hasFlag(F_MASC)) {
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return (float)Math.round(entity.getWeight() / 25.0f);
            }
            else {
                return (float)Math.round(entity.getWeight() / 20.0f);
            }
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
		} else if (hasFlag(F_DOUBLE_HEAT_SINK) && entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
			return 2;
        } else if (hasFlag(F_MASC)) {
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return Math.round(entity.getWeight() / 25.0f);
            }
            else {
                return Math.round(entity.getWeight() / 20.0f);
            }
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
        EquipmentType.addType(createISDoubleHeatSink());
        EquipmentType.addType(createCLDoubleHeatSink());
        EquipmentType.addType(createISCASE());
        EquipmentType.addType(createCLCASE());
        EquipmentType.addType(createISMASC());
        EquipmentType.addType(createCLMASC());
        EquipmentType.addType(createTSM());
        EquipmentType.addType(createC3S());
        EquipmentType.addType(createC3M());
        EquipmentType.addType(createC3I());
// + HentaiZonga
// - HentaiZonga
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
        misc.internalName = "ISDoubleHeatSink";
        misc.mepName = "IS Double Heat Sink";
        misc.mtfName = "ISDouble Heat Sink";
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "CLDoubleHeatSink";
        misc.mepName = "Clan Double Heat Sink";
        misc.mtfName = "CLDouble Heat Sink";
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
        misc.hittable = false;
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
        misc.hittable = false;
        misc.flags |= F_CASE;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.internalName = "ISMASC";
        misc.mepName = misc.name;
        misc.mtfName = misc.internalName;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.internalName = "CLMASC";
        misc.mepName = misc.name;
        misc.mtfName = misc.internalName;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createTSM() {
        MiscType misc = new MiscType();
        
        misc.name = "TSM";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "Triple Strength Myomer";
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_TSM;
        misc.bv = 0;
        
        return misc;
    }

// + HentaiZonga    
    public static MiscType createC3S() {
        MiscType misc = new MiscType();
        
        misc.name = "C3 Slave";
        misc.internalName = "ISC3SlaveUnit";
        misc.mepName = misc.name;
        misc.mtfName = "ISC3SlaveUnit";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3S;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3M() {
        MiscType misc = new MiscType();
        
        misc.name = "C3 Master";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "ISC3MasterComputer";
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3M;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3I() {
        MiscType misc = new MiscType();
        
        misc.name = "C3i Computer";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "ISImprovedC3CPU";
        misc.tonnage = 2.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3I;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createGECM() {
        MiscType misc = new MiscType();
        
        misc.name = "Guardian ECM Suite";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "ISGuardianECM";
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createECM() {
        MiscType misc = new MiscType();
        
        misc.name = "ECM Suite";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "CLECMSuite";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 0;
        
        return misc;
    }
// - HentaiZonga
}
